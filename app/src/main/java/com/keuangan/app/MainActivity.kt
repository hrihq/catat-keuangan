package com.keuangan.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var db: TransactionDb
    private var currentFragment = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = TransactionDb(this)

        if (savedInstanceState == null) {
            initFragments()
        }

        setupTabListeners()
    }

    private fun initFragments() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragmentContainer, HomeFragment(), "home")
        transaction.add(R.id.fragmentContainer, StatisticsFragment(), "stats")
        transaction.add(R.id.fragmentContainer, WalletFragment(), "wallet")
        transaction.add(R.id.fragmentContainer, ProfileFragment(), "profile")
        transaction.hide(supportFragmentManager.findFragmentByTag("stats")!!)
        transaction.hide(supportFragmentManager.findFragmentByTag("wallet")!!)
        transaction.hide(supportFragmentManager.findFragmentByTag("profile")!!)
        transaction.commit()
    }

    private fun showFragment(tag: String) {
        if (currentFragment == tag) return

        val transaction = supportFragmentManager.beginTransaction()
        val currentFrag = supportFragmentManager.findFragmentByTag(currentFragment)
        val nextFrag = supportFragmentManager.findFragmentByTag(tag)

        if (currentFrag != null) transaction.hide(currentFrag)
        if (nextFrag != null) transaction.show(nextFrag)

        transaction.commit()
        currentFragment = tag
        updateTabIndicator(tag)
    }

    private fun updateTabIndicator(tag: String) {
        val tabHome = findViewById<View>(R.id.tabHome)
        val tabStats = findViewById<View>(R.id.tabStats)
        val tabWallet = findViewById<View>(R.id.tabWallet)
        val tabProfile = findViewById<View>(R.id.tabProfile)

        val indicatorHome = findViewById<View>(R.id.indicatorHome)
        val indicatorStats = findViewById<View>(R.id.indicatorStats)
        val indicatorWallet = findViewById<View>(R.id.indicatorWallet)
        val indicatorProfile = findViewById<View>(R.id.indicatorProfile)

        listOf(indicatorHome, indicatorStats, indicatorWallet, indicatorProfile).forEach {
            it.visibility = View.GONE
        }

        when (tag) {
            "home" -> indicatorHome.visibility = View.VISIBLE
            "stats" -> indicatorStats.visibility = View.VISIBLE
            "wallet" -> indicatorWallet.visibility = View.VISIBLE
            "profile" -> indicatorProfile.visibility = View.VISIBLE
        }
    }

    private fun setupTabListeners() {
        findViewById<View>(R.id.tabHome).setOnClickListener { showFragment("home") }
        findViewById<View>(R.id.tabStats).setOnClickListener { showFragment("stats") }
        findViewById<View>(R.id.tabWallet).setOnClickListener { showFragment("wallet") }
        findViewById<View>(R.id.tabProfile).setOnClickListener { showFragment("profile") }
        findViewById<View>(R.id.fabAdd).setOnClickListener { showAddTransactionBottomSheet() }
    }

    private fun showAddTransactionBottomSheet() {
        // TODO: Show bottom sheet transaction form
    }
}
