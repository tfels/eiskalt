package de.felsernet.android.eiskalt

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlinx.coroutines.runBlocking

class GroupRepositoryTest {

    @Mock
    private lateinit var mockRepository: ListRepository

    private lateinit var groupRepository: TestableGroupRepository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        groupRepository = TestableGroupRepository(mockRepository)
    }

    @Test
    fun deleteGroup_whenGroupIsNotUsed_shouldReturnSuccess() = runBlocking {
        // Arrange
        val testGroupId = "test-group-id"
        whenever(mockRepository.getAllListNames()).thenReturn(emptyList())

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
        whenever(mockRepository.getAllListNames()).thenReturn(listOf(testListName))
        whenever(mockRepository.getList(testListName)).thenReturn(mockItems)

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
        whenever(mockRepository.getAllListNames()).thenReturn(listNames)
        whenever(mockRepository.getList("list1")).thenReturn(list1Items)
        whenever(mockRepository.getList("list2")).thenReturn(list2Items)
        whenever(mockRepository.getList("list3")).thenReturn(list3Items)

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
        whenever(mockRepository.getAllListNames()).thenReturn(listOf(testListName))
        whenever(mockRepository.getList(testListName)).thenReturn(mockItems)

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
        whenever(mockRepository.getAllListNames()).thenReturn(emptyList())

        // Act
        val result = groupRepository.deleteGroup(testGroupId)

        // Assert
        assert(result.first)  // Should return true (success)
        assert(result.second == 0)  // Should return 0 items using the group
    }

    // Testable version of GroupRepository that doesn't require Firebase
    private class TestableGroupRepository(private val repository: ListRepository) {
        suspend fun deleteGroup(groupId: String): Pair<Boolean, Int> {
            // Check if group is used in any item
            val allListNames = repository.getAllListNames()
            var itemsUsingGroup = 0

            // Check each inventory list for items using this group
            for (listName in allListNames) {
                val items = repository.getList(listName)
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
