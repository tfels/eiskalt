package de.felsernet.android.eiskalt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.snackbar.Snackbar
import de.felsernet.android.eiskalt.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AuthManager.initialize()
        SharedPreferencesHelper.initialize(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        AuthManager.authError.observe(this) { error ->
            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
        }

        AuthManager.signInWithGoogle(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        if (AuthManager.handleSignInResult(requestCode, resultCode, data)) {
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_debug_add_sample_data -> {
                addSampleData()
                true
            }
            R.id.action_relogin -> {
                AuthManager.signInWithGoogle(this)
                true
            }
            R.id.action_groups -> {
                findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.GroupListFragment)
                true
            }
            R.id.action_db_statistics -> {
                findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.DbStatisticsFragment)
                true
            }
            R.id.action_settings -> {
                findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.SettingsFragment)
                true
            }
            R.id.action_exit -> {
                finishAndRemoveTask()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Adds sample data to the database for debugging purposes
     */
    private fun addSampleData() {
        lifecycleScope.launch {
            try {
                // Add sample lists with items
                val sampleData = mapOf(
                    "Groceries" to listOf("Milk", "Eggs", "Bread", "Cheese", "Apples"),
                    "Shopping" to listOf("Shirt", "Pants", "Shoes", "Socks", "Hat"),
                    "Work Items" to listOf("Laptop", "Notebook", "Pen", "Charger", "Headphones"),
                    "Personal" to listOf("Book", "Gym Bag", "Water Bottle", "Keys", "Wallet")
                )

                for ((listName, itemNames) in sampleData) {
                    try {
                        // Create the list with items
                        val items = itemNames.map { name ->
                            Item(name = name, quantity = 1)
                        }
                        val listRepository = ListRepository()
                        listRepository.save(listName)
                        val itemRepository = ItemRepository(listName)
                        for (item in items) {
                            itemRepository.save(item)
                        }
                    } catch (e: Exception) {
                        // List might already exist, that's okay
                    }
                }

                // Show success message
                Snackbar.make(
                    binding.root,
                    "Sample data added successfully!",
                    Snackbar.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "Failed to add sample data: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

}
