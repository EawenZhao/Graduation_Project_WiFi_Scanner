package com.yinhuanzhao.graduation_project_wifi_scanner;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;


import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.yinhuanzhao.graduation_project_wifi_scanner.fragment.DatabaseFragment;
import com.yinhuanzhao.graduation_project_wifi_scanner.fragment.ScanFragment;
import com.yinhuanzhao.graduation_project_wifi_scanner.pageradapter.MyPagerAdapter;


public class MainActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private MyPagerAdapter pagerAdapter;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        pagerAdapter.addFragment(new ScanFragment(), "扫描数据");
        pagerAdapter.addFragment(new DatabaseFragment(), "查看指纹库");

        viewPager.setAdapter(pagerAdapter);

        // ViewPager 页面变化时更新 BottomNavigationView 的选中状态
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    bottomNavigationView.setSelectedItemId(R.id.navigation_scan);
                } else if (position == 1) {
                    bottomNavigationView.setSelectedItemId(R.id.navigation_database);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });

        // BottomNavigationView 菜单项点击事件处理
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_scan) {
                viewPager.setCurrentItem(0);
                return true;
            } else if (itemId == R.id.navigation_database) {
                viewPager.setCurrentItem(1);
                return true;
            } else {
                return false;
            }
        });


    }
}
