package com.bitflaker.lucidsourcekit.setup

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.bitflaker.lucidsourcekit.R
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreKeys
import com.bitflaker.lucidsourcekit.data.datastore.updateSetting
import com.bitflaker.lucidsourcekit.data.enums.AuthTypes
import com.bitflaker.lucidsourcekit.databinding.ActivitySetupViewerBinding
import com.bitflaker.lucidsourcekit.utils.Crypt
import com.bitflaker.lucidsourcekit.utils.attrColorStateList
import com.bitflaker.lucidsourcekit.utils.dpToPx
import com.bitflaker.lucidsourcekit.utils.resolveDrawable
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private const val PAGE_LABEL_LANGUAGE = "Language"
private const val PAGE_LABEL_CONSENT = "Consent"
private const val PAGE_LABEL_OPEN_SOURCE = "OpenSource"
private const val PAGE_LABEL_PRIVACY = "Privacy"
private const val PAGE_LABEL_AUTHENTICATION = "AuthData"

class SetupViewer : AppCompatActivity() {
    private val pageDots = ArrayList<ImageView>()
    private val consented = AtomicBoolean(false)
    private var selectedAuthType = AuthTypes.Pin

    private lateinit var vpAdapter: ViewPagerAdapter
    private val pageLanguage = SetupLanguageView()
    private val pageConsent = SetupConsentView()
    private val pageOpenSource = SetupOpenSourceView()
    private val pagePrivacy = SetupPrivacyView()
    private val pageAuthentication = SetupAuthDataView()
    private val pageCount = 5

    private val tabOpenSource = AtomicReference<TabLayout.Tab>()
    private val tabPrivacy = AtomicReference<TabLayout.Tab>()
    private val tabAuthData = AtomicReference<TabLayout.Tab>()

    private lateinit var binding: ActivitySetupViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySetupViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        vpAdapter = ViewPagerAdapter(supportFragmentManager, lifecycle)

        // Add base fragments and tabs
        vpAdapter.addFragment(pageLanguage, PAGE_LABEL_LANGUAGE)
        vpAdapter.addFragment(pageConsent, PAGE_LABEL_CONSENT)

        binding.tablayout.addTab(binding.tablayout.newTab().setText(PAGE_LABEL_LANGUAGE))
        binding.tablayout.addTab(binding.tablayout.newTab().setText(PAGE_LABEL_CONSENT))

        binding.viewpager.setAdapter(vpAdapter)

        // Create page indicators and select current page
        repeat(pageCount) {
            val dotPage = ImageView(this).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                backgroundTintList = attrColorStateList(R.attr.colorSurfaceContainerHigh)
                setBackgroundResource(R.drawable.page_dot)
            }
            val dp8 = 8.dpToPx
            val dp2 = 2.dpToPx
            val lp = RelativeLayout.LayoutParams(dp8, dp8).apply {
                setMargins(dp2, 0, dp2, 0)
            }
            binding.pageDotContainer.addView(dotPage, lp)
            pageDots.add(dotPage)
        }
        pageDots[0].backgroundTintList = attrColorStateList(R.attr.colorPrimary)

        // Setup page action listeners
        pageLanguage.onLanguageChangedListener = ::languageChanged
        pageConsent.setOnConsentChangedListener(::consentChanged)
        pagePrivacy.setOnAuthTypeChangedListener { authType ->
            selectedAuthType = authType
            pageAuthentication.showCredentialsSetup(authType)
        }

        // Setup viewer action listeners
        binding.tablayout.addOnTabSelectedListener(tabSelected())
        binding.viewpager.registerOnPageChangeCallback(changeTab())
        binding.btnNext.setOnClickListener(::progressToNextPage)
    }

    private fun progressToNextPage(view: View) {
        // If the current page is the last one (the authentication configuration page) then store
        // the authentication and proceed to the get started page to finish the setup process
        if (vpAdapter.isCurrentPage(PAGE_LABEL_AUTHENTICATION, binding.tablayout.selectedTabPosition)) {
            lifecycleScope.launch(Dispatchers.IO) {
                when (selectedAuthType) {
                    AuthTypes.Password -> storePassword()
                    AuthTypes.Pin -> storePIN()
                    AuthTypes.None -> storeWithoutAuthentication()
                }
                updateSetting(DataStoreKeys.APP_SETUP_FINISHED, true)
                runOnUiThread {
                    startActivity(Intent(this@SetupViewer, SetupGetStartedView::class.java).apply {
                        setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    })
                    finish()
                }
            }
            return
        }

        // Ensure the next page is in a valid range
        val nextPagePosition = binding.tablayout.selectedTabPosition + 1
        if (nextPagePosition >= vpAdapter.itemCount) {
            return
        }

        // Move to the next page
        binding.tablayout.selectTab(binding.tablayout.getTabAt(nextPagePosition))
    }

    private suspend fun storeWithoutAuthentication() {
        updateSetting(DataStoreKeys.AUTHENTICATION_TYPE, "none")
    }

    private suspend fun storePIN() {
        if (pageAuthentication.pin.isEmpty()) {
            Toast.makeText(this@SetupViewer, getResources().getString(R.string.pin_too_short), Toast.LENGTH_SHORT).show()
            return
        }
        val secretKey = Crypt.generateSecretKey()
        val pinCipher = Crypt.encryptStringBlowfish(pageAuthentication.pin, secretKey)
        updateSetting(DataStoreKeys.AUTHENTICATION_TYPE, "pin")
        updateSetting(DataStoreKeys.AUTHENTICATION_HASH, pinCipher)
        updateSetting(DataStoreKeys.AUTHENTICATION_SALT, Base64.encodeToString(secretKey, Base64.NO_WRAP))
        updateSetting(DataStoreKeys.AUTHENTICATION_USE_BIOMETRICS, pagePrivacy.useBiometrics)
    }

    private suspend fun storePassword() {
        if (pageAuthentication.password.isEmpty()) {
            Toast.makeText(this@SetupViewer, getResources().getString(R.string.password_too_short), Toast.LENGTH_SHORT).show()
            return
        }
        val salt = Crypt.generateSalt()
        val pwHash = Crypt.encryptString(pageAuthentication.password, salt)
        updateSetting(DataStoreKeys.AUTHENTICATION_TYPE, "password")
        updateSetting(DataStoreKeys.AUTHENTICATION_HASH, pwHash)
        updateSetting(DataStoreKeys.AUTHENTICATION_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
        updateSetting(DataStoreKeys.AUTHENTICATION_USE_BIOMETRICS, pagePrivacy.useBiometrics)
    }

    private fun changeTab(): OnPageChangeCallback = object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            binding.tablayout.selectTab(binding.tablayout.getTabAt(position))
        }
    }

    private fun tabSelected(): OnTabSelectedListener = object : OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
            binding.viewpager.currentItem = tab.position
            pageDots[tab.position].backgroundTintList = attrColorStateList(R.attr.colorPrimary)

            val isLastPage = binding.tablayout.selectedTabPosition + 1 == pageCount
            if (isLastPage) {
                binding.btnNext.setText(R.string.setup_next_finish)
                binding.btnNext.setIcon(null)
            } else {
                binding.btnNext.setText(R.string.setup_next)
                binding.btnNext.setIcon(resolveDrawable(R.drawable.rounded_chevron_right_24))
            }
        }

        override fun onTabUnselected(tab: TabLayout.Tab) {
            pageDots[tab.position].backgroundTintList = attrColorStateList(R.attr.colorSurfaceContainerHigh)
        }

        override fun onTabReselected(tab: TabLayout.Tab) { }
    }

    private fun consentChanged(currentConsent: Boolean) {
        if (currentConsent) {
            vpAdapter.addFragment(pageOpenSource, PAGE_LABEL_OPEN_SOURCE)
            vpAdapter.addFragment(pagePrivacy, PAGE_LABEL_PRIVACY)
            vpAdapter.addFragment(pageAuthentication, PAGE_LABEL_AUTHENTICATION)

            tabOpenSource.set(binding.tablayout.newTab().setText(PAGE_LABEL_OPEN_SOURCE))
            tabPrivacy.set(binding.tablayout.newTab().setText(PAGE_LABEL_PRIVACY))
            tabAuthData.set(binding.tablayout.newTab().setText(PAGE_LABEL_AUTHENTICATION))

            binding.tablayout.addTab(tabOpenSource.get())
            binding.tablayout.addTab(tabPrivacy.get())
            binding.tablayout.addTab(tabAuthData.get())
        } else {
            vpAdapter.removeFragment(pageOpenSource, PAGE_LABEL_OPEN_SOURCE)
            vpAdapter.removeFragment(pagePrivacy, PAGE_LABEL_PRIVACY)
            vpAdapter.removeFragment(pageAuthentication, PAGE_LABEL_AUTHENTICATION)

            binding.tablayout.removeTab(tabOpenSource.get())
            binding.tablayout.removeTab(tabPrivacy.get())
            binding.tablayout.removeTab(tabAuthData.get())

        }
        consented.set(currentConsent)
    }

    private fun languageChanged() = runOnUiThread {
        binding.btnNext.text = resources.getString(R.string.setup_next)
        (vpAdapter.getFragment(PAGE_LABEL_LANGUAGE) as SetupLanguageView).onLanguageUpdated()
        (vpAdapter.getFragment(PAGE_LABEL_CONSENT) as SetupConsentView).onLanguageUpdated()
        if (consented.get()) {
            (vpAdapter.getFragment(PAGE_LABEL_OPEN_SOURCE) as SetupOpenSourceView).onLanguageUpdated()
            (vpAdapter.getFragment(PAGE_LABEL_PRIVACY) as SetupPrivacyView).onLanguageUpdated()
            (vpAdapter.getFragment(PAGE_LABEL_AUTHENTICATION) as SetupAuthDataView).onLanguageUpdated()
        }
    }
}