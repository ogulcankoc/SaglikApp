package com.example.saglikapp.ui;

import androidx.viewpager2.widget.ViewPager2;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.saglikapp.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.os.Build;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

public class WelcomeActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Tam ekran (Sürükleyici Mod) özelliğini aktif et
        hideSystemUI();

        viewPager = findViewById(R.id.view_pager);
        bottomNav = findViewById(R.id.bottom_navigation);

        // Sayfaları yöneten adapter'ı kuruyoruz
        MainViewPagerAdapter adapter = new MainViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // 1. DURUM: Alt menüdeki (Instagram tarzı) bir öğeye basıldığında ilgili sayfaya git
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) viewPager.setCurrentItem(0);
            else if (itemId == R.id.nav_water) viewPager.setCurrentItem(1);
            else if (itemId == R.id.nav_sleep) viewPager.setCurrentItem(2);
            else if (itemId == R.id.nav_heart) viewPager.setCurrentItem(3);
            else if (itemId == R.id.nav_bmi) viewPager.setCurrentItem(4);
            return true;
        });

        // 2. DURUM: Ekran parmakla kaydırıldığında (Swipe) alt menüdeki seçili ikonu güncelle
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 0: bottomNav.setSelectedItemId(R.id.nav_home); break;
                    case 1: bottomNav.setSelectedItemId(R.id.nav_water); break;
                    case 2: bottomNav.setSelectedItemId(R.id.nav_sleep); break;
                    case 3: bottomNav.setSelectedItemId(R.id.nav_heart); break;
                    case 4: bottomNav.setSelectedItemId(R.id.nav_bmi); break;
                }
            }
        });

        // Tüm sekmelerin (5 sekme) bellekte hazır tutulmasını sağlar (Akıcı geçiş için)
        viewPager.setOffscreenPageLimit(4);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Uygulama odağı kazandığında (örn. geri dönüldüğünde) çubukları tekrar gizle
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                // Hem durum çubuğunu hem navigasyon çubuğunu gizle
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                // Sadece kenardan çekince geçici olarak görünmesini sağla
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // Eski Android sürümleri için destek
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }
}