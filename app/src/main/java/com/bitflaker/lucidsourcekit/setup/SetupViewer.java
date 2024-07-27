package com.bitflaker.lucidsourcekit.setup;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.data.enums.AuthTypes;
import com.bitflaker.lucidsourcekit.utils.Crypt;
import com.bitflaker.lucidsourcekit.utils.Tools;
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreKeys;
import com.bitflaker.lucidsourcekit.data.datastore.DataStoreManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SetupViewer extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager2;
    private LinearLayout pageDotContainer;
    private final ArrayList<ImageView> pageDots = new ArrayList<>();
    private AuthTypes selectedAuthType = AuthTypes.Pin;

    private final AtomicBoolean consented = new AtomicBoolean(false);
    private final int pageCount = 5;
    private final String pageLang = "Language";
    private final String pageConsent = "Consent";
    private final String pageOpenSource = "OpenSource";
    private final String pagePrivacy = "Privacy";
    private final String pageAuthData = "AuthData";

    private ViewPagerAdapter vpAdapter;
    private final SetupLanguageView setLang = new SetupLanguageView();
    private final SetupConsentView setConsent = new SetupConsentView();
    private final SetupOpenSourceView setOpenSource = new SetupOpenSourceView();
    private final SetupPrivacyView setPrivacy = new SetupPrivacyView();
    private final SetupAuthDataView setAuthData = new SetupAuthDataView();

    private final AtomicReference<TabLayout.Tab> tabOpenSource = new AtomicReference<>();
    private final AtomicReference<TabLayout.Tab> tabPrivacy = new AtomicReference<>();
    private final AtomicReference<TabLayout.Tab> tabAuthData = new AtomicReference<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        setTheme(Tools.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_viewer);
        Tools.makeStatusBarTransparent(this);

        tabLayout = findViewById(R.id.tablayout);
        viewPager2 = findViewById(R.id.viewpager);
        MaterialButton btnNext = findViewById(R.id.btn_next);
        pageDotContainer = findViewById(R.id.page_dot_container);

        setPrivacy.setOnAuthTypeChangedListener(authType -> {
            selectedAuthType = authType;
            setAuthData.showCredentialsSetup(authType);
        });

        setLang.setLanguageChangedListener(this::languageChanged);
        setConsent.setOnConsentChangedListener(this::consentChanged);

        vpAdapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        vpAdapter.addFragment(setLang, pageLang);
        vpAdapter.addFragment(setConsent, pageConsent);
        viewPager2.setAdapter(vpAdapter);

        tabLayout.addTab(tabLayout.newTab().setText(pageLang));
        tabLayout.addTab(tabLayout.newTab().setText(pageConsent));

        addProgressDots();

        tabLayout.addOnTabSelectedListener(tabSelected());
        viewPager2.registerOnPageChangeCallback(changeTab());
        btnNext.setOnClickListener(this::progressToNextPage);
    }

    private void progressToNextPage(View view) {
        // Invalid pages
        if (tabLayout.getSelectedTabPosition() < 0 || tabLayout.getSelectedTabPosition() >= vpAdapter.getItemCount()) {
            Log.e("SetupViewer", "Invalid tab position \"" + tabLayout.getSelectedTabPosition() + "\" on item count \"" + vpAdapter.getItemCount() + "\"");
            return;
        }

        // Handle every page except the last one
        if (tabLayout.getSelectedTabPosition() + 1 < vpAdapter.getItemCount()) {
            tabLayout.selectTab(tabLayout.getTabAt(tabLayout.getSelectedTabPosition() + 1));
            return;
        }

        // Handle the last page
        handleAuthenticationPage();
    }

    private void handleAuthenticationPage() {
        try {
            switch (selectedAuthType) {
                case Password: storePassword(); break;
                case Pin: storePIN(); break;
                case None: storeWithoutAuthentication(); break;
            }
            startGetStarted();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void storeWithoutAuthentication() {
        DataStoreManager dsManager = DataStoreManager.getInstance();
        dsManager.updateSetting(DataStoreKeys.AUTHENTICATION_TYPE, "none").blockingSubscribe();
    }

    private void storePIN() throws NoSuchAlgorithmException {
        if (setAuthData.getPin().length() == 0) {
            Toast.makeText(SetupViewer.this, getResources().getString(R.string.pin_too_short), Toast.LENGTH_SHORT).show();
            return;
        }
        DataStoreManager dsManager = DataStoreManager.getInstance();
        byte[] secretKey = Crypt.generateSecretKey();
        String pinCipher = Crypt.encryptStringBlowfish(setAuthData.getPin(), secretKey);
        dsManager.updateSetting(DataStoreKeys.AUTHENTICATION_TYPE, "pin").blockingSubscribe();
        dsManager.updateSetting(DataStoreKeys.AUTHENTICATION_HASH, pinCipher).blockingSubscribe();
        dsManager.updateSetting(DataStoreKeys.AUTHENTICATION_SALT, Base64.encodeToString(secretKey, Base64.NO_WRAP)).blockingSubscribe();
        dsManager.updateSetting(DataStoreKeys.AUTHENTICATION_USE_BIOMETRICS, setPrivacy.getUseBiometrics()).blockingSubscribe();
    }

    private void storePassword() throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (setAuthData.getPassword().length() == 0) {
            Toast.makeText(SetupViewer.this, getResources().getString(R.string.password_too_short), Toast.LENGTH_SHORT).show();
            return;
        }
        DataStoreManager dsManager = DataStoreManager.getInstance();
        byte[] salt = Crypt.generateSalt();
        String pwHash = Crypt.encryptString(setAuthData.getPassword(), salt);
        dsManager.updateSetting(DataStoreKeys.AUTHENTICATION_TYPE, "password").blockingSubscribe();
        dsManager.updateSetting(DataStoreKeys.AUTHENTICATION_HASH, pwHash).blockingSubscribe();
        dsManager.updateSetting(DataStoreKeys.AUTHENTICATION_SALT, Base64.encodeToString(salt, Base64.NO_WRAP)).blockingSubscribe();
        dsManager.updateSetting(DataStoreKeys.AUTHENTICATION_USE_BIOMETRICS, setPrivacy.getUseBiometrics()).blockingSubscribe();
    }

    @NonNull
    private ViewPager2.OnPageChangeCallback changeTab() {
        return new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tabLayout.selectTab(tabLayout.getTabAt(position));
            }
        };
    }

    @NonNull
    private TabLayout.OnTabSelectedListener tabSelected() {
        return new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager2.setCurrentItem(tab.getPosition());
                pageDots.get(tab.getPosition()).setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorPrimary, getTheme()));

                if (tabLayout.getSelectedTabPosition() + 1 == pageCount) {
                    ((MaterialButton) findViewById(R.id.btn_next)).setText(R.string.setup_next_finish);
                    ((MaterialButton) findViewById(R.id.btn_next)).setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                }
                else {
                    ((MaterialButton) findViewById(R.id.btn_next)).setText(R.string.setup_next);
                    ((MaterialButton) findViewById(R.id.btn_next)).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_arrow_forward_24, 0);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                pageDots.get(tab.getPosition()).setBackgroundTintList(Tools.getAttrColorStateList(R.attr.colorSurfaceContainerLow, getTheme()));
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        };
    }

    private void addProgressDots() {
        for (int i = 0; i < pageCount; i++) {
            ImageView dotPage = new ImageView(this);
            dotPage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            dotPage.setBackgroundResource(R.drawable.page_dot);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(Tools.dpToPx(this, 8), Tools.dpToPx(this, 8));
            lp.setMargins(Tools.dpToPx(this, 2), 0, Tools.dpToPx(this, 2), 0);
            dotPage.setBackgroundTintList(Tools.getAttrColorStateList(i == 0 ? R.attr.colorPrimary : R.attr.colorSurfaceContainerLow, getTheme()));
            pageDotContainer.addView(dotPage, lp);
            pageDots.add(dotPage);
        }
    }

    private void consentChanged(boolean currentConsent) {
        if (currentConsent) {
            vpAdapter.addFragment(setOpenSource, pageOpenSource);
            vpAdapter.addFragment(setPrivacy, pagePrivacy);
            vpAdapter.addFragment(setAuthData, pageAuthData);

            tabOpenSource.set(tabLayout.newTab().setText(pageOpenSource));
            tabPrivacy.set(tabLayout.newTab().setText(pagePrivacy));
            tabAuthData.set(tabLayout.newTab().setText(pageAuthData));

            tabLayout.addTab(tabOpenSource.get());
            tabLayout.addTab(tabPrivacy.get());
            tabLayout.addTab(tabAuthData.get());

            consented.set(true);
        }
        else {
            vpAdapter.removeFragment(setOpenSource, pageOpenSource);
            vpAdapter.removeFragment(setPrivacy, pagePrivacy);
            vpAdapter.removeFragment(setAuthData, pageAuthData);

            tabLayout.removeTab(tabOpenSource.get());
            tabLayout.removeTab(tabPrivacy.get());
            tabLayout.removeTab(tabAuthData.get());

            consented.set(false);
        }
    }

    private void languageChanged(String langCode) {
        ((MaterialButton) findViewById(R.id.btn_next)).setText(getResources().getString(R.string.setup_next));
        ((SetupLanguageView) vpAdapter.getFragment(pageLang)).updateLanguages();
        ((SetupConsentView) vpAdapter.getFragment(pageConsent)).updateLanguages();
        if (consented.get()) {
            ((SetupOpenSourceView) vpAdapter.getFragment(pageOpenSource)).updateLanguages();
            ((SetupPrivacyView) vpAdapter.getFragment(pagePrivacy)).updateLanguages();
            ((SetupAuthDataView) vpAdapter.getFragment(pageAuthData)).updateLanguages();
        }
    }

    public void startGetStarted() {
        DataStoreManager.getInstance().updateSetting(DataStoreKeys.APP_SETUP_FINISHED, true).blockingSubscribe();
        Intent intent = new Intent(SetupViewer.this, SetupGetStartedView.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}