package de.felsernet.android.eiskalt

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import de.felsernet.android.eiskalt.common.AuthManager
import de.felsernet.android.eiskalt.common.VersionCheckManager
import de.felsernet.android.eiskalt.databinding.ActivityMainBinding
import de.felsernet.android.eiskalt.log.LogDialogFragment
import de.felsernet.android.eiskalt.log.LogMenuIndicator
import de.felsernet.android.eiskalt.settings.AboutDialogFragment

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private lateinit var binding: ActivityMainBinding
    private lateinit var versionCheckManager: VersionCheckManager
    private lateinit var logMenuIndicator: LogMenuIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize version check manager
        versionCheckManager = VersionCheckManager(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // with FragmentContainerView the NavController has to be taken from the NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        AuthManager.authError.observe(this) { error ->
            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
        }

        // Show an indicator on the menu while there are unseen errors in the log
        logMenuIndicator = LogMenuIndicator(this, binding.toolbar)
        logMenuIndicator.attach()

        // Check for updates on startup
        versionCheckManager.checkForUpdates(this)

        AuthManager.signInWithGoogle(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Handle version check result
        versionCheckManager.onActivityResult(requestCode, resultCode, data)

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

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        logMenuIndicator.updateMenu(menu)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_relogin -> {
                AuthManager.reconnect(this)
                true
            }
            R.id.action_groups -> {
                navController.navigate(R.id.GroupListFragment)
                true
            }
            R.id.action_db_statistics -> {
                navController.navigate(R.id.DbStatisticsFragment)
                true
            }
            R.id.action_log -> {
                LogDialogFragment().show(supportFragmentManager, LogDialogFragment.TAG)
                true
            }
            R.id.action_settings -> {
                navController.navigate(R.id.SettingsFragment)
                true
            }
            R.id.action_about -> {
                AboutDialogFragment().show(supportFragmentManager, AboutDialogFragment.TAG)
                true
            }
            R.id.action_exit -> {
                finishAndRemoveTask()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

}
