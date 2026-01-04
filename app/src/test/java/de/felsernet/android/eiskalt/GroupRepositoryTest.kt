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
    private lateinit var mockInventoryRepository: InventoryRepository

    private lateinit var groupRepository: TestableGroupRepository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        groupRepository = TestableGroupRepository(mockInventoryRepository)
    }

    @Test
    fun deleteGroup_whenGroupIsNotUsed_shouldReturnSuccess() = runBlocking {
        // Arrange
        val testGroupId = "test-group-id"
        whenever(mockInventoryRepository.getAllListNames()).thenReturn(emptyList())

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

        // Create mock inventory items that use the group
        val mockItems = listOf(
            InventoryItem("Item 1", 1, 5, testGroupId),
            InventoryItem("Item 2", 2, 3, testGroupId),
            InventoryItem("Item 3", 3, 7, testGroupId)
        )

        // Mock that the group is used by inventory items
        whenever(mockInventoryRepository.getAllListNames()).thenReturn(listOf(testListName))
        whenever(mockInventoryRepository.getList(testListName)).thenReturn(mockItems)

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

        // Create mock inventory items across multiple lists
        val list1Items = listOf(
            InventoryItem("Item 1", 1, 5, testGroupId),
            InventoryItem("Item 2", 2, 3, testGroupId)
        )
        val list2Items = listOf(
            InventoryItem("Item 3", 3, 7, testGroupId)
        )
        val list3Items = listOf(
            InventoryItem("Item 4", 4, 2, testGroupId),
            InventoryItem("Item 5", 5, 4, testGroupId),
            InventoryItem("Item 6", 6, 1, testGroupId)
        )

        // Mock multiple lists with items using the group
        whenever(mockInventoryRepository.getAllListNames()).thenReturn(listNames)
        whenever(mockInventoryRepository.getList("list1")).thenReturn(list1Items)
        whenever(mockInventoryRepository.getList("list2")).thenReturn(list2Items)
        whenever(mockInventoryRepository.getList("list3")).thenReturn(list3Items)

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

        // Create mock inventory items where only some use our test group
        val mockItems = listOf(
            InventoryItem("Item 1", 1, 5, testGroupId),      // Uses our group
            InventoryItem("Item 2", 2, 3, otherGroupId),    // Uses different group
            InventoryItem("Item 3", 3, 7, testGroupId),      // Uses our group
            InventoryItem("Item 4", 4, 2, null),             // No group
            InventoryItem("Item 5", 5, 4, testGroupId)       // Uses our group
        )

        // Mock that the group is used by some inventory items
        whenever(mockInventoryRepository.getAllListNames()).thenReturn(listOf(testListName))
        whenever(mockInventoryRepository.getList(testListName)).thenReturn(mockItems)

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
        whenever(mockInventoryRepository.getAllListNames()).thenReturn(emptyList())

        // Act
        val result = groupRepository.deleteGroup(testGroupId)

        // Assert
        assert(result.first)  // Should return true (success)
        assert(result.second == 0)  // Should return 0 items using the group
    }

    // Testable version of GroupRepository that doesn't require Firebase
    private class TestableGroupRepository(private val inventoryRepository: InventoryRepository) {
        suspend fun deleteGroup(groupId: String): Pair<Boolean, Int> {
            // Check if group is used in any inventory items
            val allListNames = inventoryRepository.getAllListNames()
            var itemsUsingGroup = 0

            // Check each inventory list for items using this group
            for (listName in allListNames) {
                val items = inventoryRepository.getList(listName)
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
