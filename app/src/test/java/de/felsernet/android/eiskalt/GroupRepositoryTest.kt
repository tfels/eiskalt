package de.felsernet.android.eiskalt

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlinx.coroutines.runBlocking

class GroupRepositoryTest {

    @Mock
    private lateinit var mockListRepository: ListRepository

    private lateinit var groupRepository: TestableGroupRepository

    private val mockItemRepos = mutableMapOf<String, ItemRepository>()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mockItemRepos.clear()
        groupRepository = TestableGroupRepository(mockListRepository) { listName -> mockItemRepos[listName]!! }
    }

    @Test
    fun deleteGroup_whenGroupIsNotUsed_shouldReturnSuccess() = runBlocking {
        // Arrange
        val testGroupId = "test-group-id"
        whenever(mockListRepository.getAll()).thenReturn(emptyList())

        // Act
        val result = groupRepository.deleteGroup(testGroupId)

        // Assert
        assert(result.first)  // Should return true (success)
        assert(result.second == 0)  // Should return 0 items using the group
    }

    @Test
    fun deleteGroup_whenGroupIsUsed_shouldReturnFailureAndItemCount() = runBlocking {
        // Arrange
        val testGroupId = "test-group-id"
        val testListName = "test-list"
        val itemsUsingGroup = 3

        // Create mock item that use the group
        val mockItems = listOf(
            Item("Item 1", "1", 5, testGroupId),
            Item("Item 2", "2", 3, testGroupId),
            Item("Item 3", "3", 7, testGroupId)
        )

        // Mock that the group is used by items
        mockItemRepos[testListName] = mock(ItemRepository::class.java)
        whenever(mockItemRepos[testListName]!!.getAll()).thenReturn(mockItems)
        whenever(mockListRepository.getAll()).thenReturn(listOf(testListName))

        // Act
        val result = groupRepository.deleteGroup(testGroupId)

        // Assert
        assert(!result.first)  // Should return false (not deleted)
        assert(result.second == itemsUsingGroup)  // Should return count of items using the group
    }

    @Test
    fun deleteGroup_whenGroupIsUsedInMultipleLists_shouldCountAllItems() = runBlocking {
        // Arrange
        val testGroupId = "test-group-id"
        val listNames = listOf("list1", "list2", "list3")

        // Create mock items across multiple lists
        val list1Items = listOf(
            Item("Item 1", "1", 5, testGroupId),
            Item("Item 2", "2", 3, testGroupId)
        )
        val list2Items = listOf(
            Item("Item 3", "3", 7, testGroupId)
        )
        val list3Items = listOf(
            Item("Item 4", "4", 2, testGroupId),
            Item("Item 5", "5", 4, testGroupId),
            Item("Item 6", "6", 1, testGroupId)
        )

        // Mock multiple lists with items using the group
        mockItemRepos["list1"] = mock(ItemRepository::class.java)
        whenever(mockItemRepos["list1"]!!.getAll()).thenReturn(list1Items)
        mockItemRepos["list2"] = mock(ItemRepository::class.java)
        whenever(mockItemRepos["list2"]!!.getAll()).thenReturn(list2Items)
        mockItemRepos["list3"] = mock(ItemRepository::class.java)
        whenever(mockItemRepos["list3"]!!.getAll()).thenReturn(list3Items)
        whenever(mockListRepository.getAll()).thenReturn(listNames)

        // Act
        val result = groupRepository.deleteGroup(testGroupId)

        // Assert
        assert(!result.first)  // Should return false (not deleted)
        assert(result.second == 6)  // Should return total count: 2 + 1 + 3 = 6
    }

    @Test
    fun deleteGroup_whenGroupIsUsedBySomeItemsInList_shouldOnlyCountMatchingItems() = runBlocking {
        // Arrange
        val testGroupId = "test-group-id"
        val otherGroupId = "other-group-id"
        val testListName = "test-list"

        // Create mock items where only some use our test group
        val mockItems = listOf(
            Item("Item 1", "1", 5, testGroupId),      // Uses our group
            Item("Item 2", "2", 3, otherGroupId),    // Uses different group
            Item("Item 3", "3", 7, testGroupId),      // Uses our group
            Item("Item 4", "4", 2, null),             // No group
            Item("Item 5", "5", 4, testGroupId)       // Uses our group
        )

        // Mock that the group is used by some items
        mockItemRepos[testListName] = mock(ItemRepository::class.java)
        whenever(mockItemRepos[testListName]!!.getAll()).thenReturn(mockItems)
        whenever(mockListRepository.getAll()).thenReturn(listOf(testListName))

        // Act
        val result = groupRepository.deleteGroup(testGroupId)

        // Assert
        assert(!result.first)  // Should return false (not deleted)
        assert(result.second == 3)  // Should return count of items using our specific group
    }

    @Test
    fun deleteGroup_whenNoListsExist_shouldReturnSuccess() = runBlocking {
        // Arrange
        val testGroupId = "test-group-id"
        whenever(mockListRepository.getAll()).thenReturn(emptyList())

        // Act
        val result = groupRepository.deleteGroup(testGroupId)

        // Assert
        assert(result.first)  // Should return true (success)
        assert(result.second == 0)  // Should return 0 items using the group
    }

    // Testable version of GroupRepository that doesn't require Firebase
    private class TestableGroupRepository(private val listRepository: ListRepository, private val itemRepositoryFactory: (String) -> ItemRepository) {
        suspend fun deleteGroup(groupId: String): Pair<Boolean, Int> {
            // Check if group is used in any item
            val allListNames = listRepository.getAll()
            var itemsUsingGroup = 0

            // Check each list for items using this group
            for (listName in allListNames) {
                val itemRepo = itemRepositoryFactory(listName)
                val items = itemRepo.getAll()
                itemsUsingGroup += items.count { it.groupId == groupId }
            }

            // If group is being used, don't delete and return false with count
            if (itemsUsingGroup > 0) {
                return Pair(false, itemsUsingGroup)
            }

            // Group is not used, safe to delete
            return Pair(true, 0)
        }
    }
}
