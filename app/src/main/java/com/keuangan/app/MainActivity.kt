package com.keuangan.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager

class MainActivity : AppCompatActivity() {

    private lateinit var db: TransactionDb
    private lateinit var fragmentManager: FragmentManager

    private var currentFragment = "home"
    private val fragments = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = TransactionDb(this)
        fragmentManager = supportFragmentManager

        if (savedInstanceState == null) {
            initFragments()
            showFragment("home")
        }

        setupTabListeners()
    }

    private fun initFragments() {
        fragments["home"] = HomeFragment::class.java.name
        fragments["stats"] = StatisticsFragment::class.java.name
        fragments["wallet"] = WalletFragment::class.java.name
        fragments["profile"] = ProfileFragment::class.java.name

        val transaction = fragmentManager.beginTransaction()
        transaction.add(R.id.fragmentContainer, HomeFragment(), "home")
        transaction.add(R.id.fragmentContainer, StatisticsFragment(), "stats")
        transaction.add(R.id.fragmentContainer, WalletFragment(), "wallet")
        transaction.add(R.id.fragmentContainer, ProfileFragment(), "profile")
        transaction.hide(fragmentManager.findFragmentByTag("stats")!!)
        transaction.hide(fragmentManager.findFragmentByTag("wallet")!!)
        transaction.hide(fragmentManager.findFragmentByTag("profile")!!)
        transaction.commit()
    }

    private fun showFragment(tag: String) {
        if (currentFragment == tag) return

        val transaction = fragmentManager.beginTransaction()
        val currentFrag = fragmentManager.findFragmentByTag(currentFragment)
        val nextFrag = fragmentManager.findFragmentByTag(tag)

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

        tabHome.alpha = if (tag == "home") 1f else 0.6f
        tabStats.alpha = if (tag == "stats") 1f else 0.6f
        tabWallet.alpha = if (tag == "wallet") 1f else 0.6f
        tabProfile.alpha = if (tag == "profile") 1f else 0.6f
    }

    private fun setupTabListeners() {
        findViewById<View>(R.id.tabHome).setOnClickListener {
            showFragment("home")
        }
        findViewById<View>(R.id.tabStats).setOnClickListener {
            showFragment("stats")
        }
        findViewById<View>(R.id.tabWallet).setOnClickListener {
            showFragment("wallet")
        }
        findViewById<View>(R.id.tabProfile).setOnClickListener {
            showFragment("profile")
        }

        findViewById<View>(R.id.fabAdd).setOnClickListener {
            showAddTransactionBottomSheet()
        }
    }

    private fun showAddTransactionBottomSheet() {
        // TODO: Full-screen transaction form
    }
}
