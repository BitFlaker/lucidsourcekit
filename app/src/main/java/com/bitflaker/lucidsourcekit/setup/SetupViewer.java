package com.bitflaker.lucidsourcekit.setup;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bitflaker.lucidsourcekit.R;
import com.bitflaker.lucidsourcekit.general.AuthTypes;
import com.bitflaker.lucidsourcekit.general.Crypt;
import com.bitflaker.lucidsourcekit.general.DatabaseWrapper;
import com.bitflaker.lucidsourcekit.general.Tools;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SetupViewer extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager2;
    private MaterialButton btnNext;
    private LinearLayout pageDotContainer;
    private ArrayList<ImageView> pageDots;
    private AuthTypes selectedAuthType = AuthTypes.Pin;

    private AtomicBoolean consented = new AtomicBoolean(false);
    private final int pageCount = 5;
    private final String pageLang = "Language";
    private final String pageConsent = "Consent";
    private final String pageOpenSource = "OpenSource";
    private final String pagePrivacy = "Privacy";
    private final String pageAuthData = "AuthData";

    private ViewPagerAdapter vpAdapter;
    private SetupLanguage setLang;
    private SetupConsent setConsent;
    private SetupOpenSource setOpenSource;
    private SetupPrivacy setPrivacy;
    private SetupAuthData setAuthData;

    private AtomicReference<TabLayout.Tab> tabOpenSource;
    private AtomicReference<TabLayout.Tab> tabPrivacy;
    private AtomicReference<TabLayout.Tab> tabAuthData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_viewer);
        Tools.makeStatusBarTransparent(this);
        initVars();

        setPrivacy.setOnAuthTypeChangedListener(authType -> {
            selectedAuthType = authType;
            switch (authType) {
                case Pin:
                    setAuthData.showPinSetup();
                    break;
                case Password:
                    setAuthData.showPasswordSetup();
                    break;
                case None:
                    // TODO: hide slide? no auth data needed to be set
                    break;
            }
        });

        setLang.setLanguageChangedListener(languageChanged());
        setConsent.setOnConsentChangedListener(consentChanged());

        vpAdapter.addFragment(setLang, pageLang);
        vpAdapter.addFragment(setConsent, pageConsent);
        viewPager2.setAdapter(vpAdapter);

        tabLayout.addTab(tabLayout.newTab().setText(pageLang));
        tabLayout.addTab(tabLayout.newTab().setText(pageConsent));

        addProgressDots();

        tabLayout.addOnTabSelectedListener(tabSelected());
        viewPager2.registerOnPageChangeCallback(changeTab());
        btnNext.setOnClickListener(progressToNextPage());
    }

    @NonNull
    private View.OnClickListener progressToNextPage() {
        return e -> {
            if (tabLayout.getSelectedTabPosition() + 1 < vpAdapter.getItemCount()) {
                tabLayout.selectTab(tabLayout.getTabAt(tabLayout.getSelectedTabPosition() + 1));
            } else if (tabLayout.getSelectedTabPosition() + 1 == pageCount) {
                DatabaseWrapper dbWrapper = new DatabaseWrapper(SetupViewer.this);
                switch (selectedAuthType) {
                    case Password:
                        if (setAuthData.getPassword().length() == 0) {
                            // TODO extract string resource
                            Toast.makeText(SetupViewer.this, "Password too short!", Toast.LENGTH_SHORT).show();
                        } else {
                            try {
                                byte[] salt = Crypt.generateSalt();
                                String pwHash = Crypt.encryptString(setAuthData.getPassword(), salt);
                                dbWrapper.SetProperty("auth_type", "password");
                                dbWrapper.SetProperty("auth_hash", pwHash);
                                dbWrapper.SetProperty("auth_salt", Base64.encodeToString(salt, Base64.NO_WRAP));
                                dbWrapper.SetProperty("auth_use_biometrics", setPrivacy.getUseBiometrics() ? "true" : "false");
                                startGetStarted();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                        break;
                    case Pin:
                        if (setAuthData.getPin().length() == 0) {
                            // TODO extract string resource
                            Toast.makeText(SetupViewer.this, "PIN too short!", Toast.LENGTH_SHORT).show();
                        } else {
                            try {
                                byte[] secretKey = Crypt.generateSecretKey();
                                String pinCipher = Crypt.encryptStringBlowfish(setAuthData.getPin(), secretKey);
                                dbWrapper.SetProperty("auth_type", "pin");
                                dbWrapper.SetProperty("auth_cipher", pinCipher);
                                dbWrapper.SetProperty("auth_key", Base64.encodeToString(secretKey, Base64.NO_WRAP));
                                dbWrapper.SetProperty("auth_use_biometrics", setPrivacy.getUseBiometrics() ? "true" : "false");
                                startGetStarted();
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                        break;
                    case None:
                        dbWrapper.SetProperty("auth_type", "none");
                        startGetStarted();
                        break;
                }
            }
        };
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
                pageDots.get(tab.getPosition()).setBackgroundTintList(Tools.getAttrColor(R.attr.activePageDot, getTheme()));

                if (tabLayout.getSelectedTabPosition() + 1 == pageCount) {
                    ((MaterialButton) findViewById(R.id.btn_next)).setText(R.string.setup_next_finish);
                    ((MaterialButton) findViewById(R.id.btn_next)).setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                } else {
                    ((MaterialButton) findViewById(R.id.btn_next)).setText(R.string.setup_next);
                    ((MaterialButton) findViewById(R.id.btn_next)).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_arrow_forward_24, 0);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                pageDots.get(tab.getPosition()).setBackgroundTintList(Tools.getAttrColor(R.attr.inactivePageDot, getTheme()));
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

            ColorStateList stateList;
            if(i == 0){ stateList = Tools.getAttrColor(R.attr.activePageDot, getTheme()); }
            else{ stateList = Tools.getAttrColor(R.attr.inactivePageDot, getTheme()); }
            dotPage.setBackgroundTintList(stateList);

            pageDotContainer.addView(dotPage, lp);
            pageDots.add(dotPage);
        }
    }

    @NonNull
    private SetupConsent.OnConsentChangedListener consentChanged() {
        return currentConsent -> {
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
            } else {
                vpAdapter.removeFragment(setOpenSource, pageOpenSource);
                vpAdapter.removeFragment(setPrivacy, pagePrivacy);
                vpAdapter.removeFragment(setAuthData, pageAuthData);

                tabLayout.removeTab(tabOpenSource.get());
                tabLayout.removeTab(tabPrivacy.get());
                tabLayout.removeTab(tabAuthData.get());

                consented.set(false);
            }
        };
    }

    @NonNull
    private SetupLanguage.OnLanguageChangedListener languageChanged() {
        return e -> {
            ((MaterialButton) findViewById(R.id.btn_next)).setText(getResources().getString(R.string.setup_next));
            ((SetupLanguage) vpAdapter.getFragment(pageLang)).updateLanguages();
            ((SetupConsent) vpAdapter.getFragment(pageConsent)).updateLanguages();
            if (consented.get()) {
                ((SetupOpenSource) vpAdapter.getFragment(pageOpenSource)).updateLanguages();
                ((SetupPrivacy) vpAdapter.getFragment(pagePrivacy)).updateLanguages();
                ((SetupAuthData) vpAdapter.getFragment(pageAuthData)).updateLanguages();
            }
        };
    }

    private void initVars() {
        tabLayout = findViewById(R.id.tablayout);
        viewPager2 = findViewById(R.id.viewpager);
        btnNext = findViewById(R.id.btn_next);
        pageDotContainer = findViewById(R.id.page_dot_container);
        pageDots = new ArrayList<>();

        vpAdapter = new ViewPagerAdapter(getSupportFragmentManager(), getLifecycle());
        setLang = new SetupLanguage();
        setConsent = new SetupConsent();
        setOpenSource = new SetupOpenSource();
        setPrivacy = new SetupPrivacy();
        setAuthData = new SetupAuthData();

        tabOpenSource = new AtomicReference<>();
        tabPrivacy = new AtomicReference<>();
        tabAuthData = new AtomicReference<>();
    }

    public void startGetStarted() {
        Intent intent = new Intent(SetupViewer.this, SetupGetStarted.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}