package com.mgke.da.ui.categories;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import com.mgke.da.R;
import com.mgke.da.databinding.FragmentCategoryBinding;

public class CategoryFragment extends Fragment {

    private FragmentCategoryBinding binding;
    private TabLayout tabLayout; // TabLayout

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCategoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Log.d("CategoryFragment", "onCreateView called"); // Лог для отладки

        // Настройка TabLayout и ViewPager
        setupViewPagerAndTabs();

        return root;
    }

    private void setupViewPagerAndTabs() {
        tabLayout = binding.tabLayout; // Инициализация tabLayout
        ViewPager viewPager = binding.viewPager;

        viewPager.setAdapter(new FragmentPagerAdapter(getChildFragmentManager()) {
            @Override
            public int getCount() {
                return 2; // Две вкладки
            }

            @Override
            public Fragment getItem(int position) {
                if (position == 0) {
                    Log.d("CategoryFragment", "Loading ExpensesFragment");
                    return new ExpensesFragment();
                } else {
                    Log.d("CategoryFragment", "Loading IncomeFragment");
                    return new IncomeFragment();
                }
            }

            @Override
            public CharSequence getPageTitle(int position) {
                if (position == 0) {
                    return getString(R.string.tab_expenses); // Название вкладки "Расходы"
                } else {
                    return getString(R.string.tab_income); // Название вкладки "Доходы"
                }
            }
        });

        tabLayout.setupWithViewPager(viewPager);

        viewPager.setCurrentItem(0); // 0 - индекс вкладки "Расходы"

        // Добавляем слушатель для переключения вкладок
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                Log.d("CategoryFragment", "Tab selected: " + position); // Лог для отладки
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Можно оставить пустым
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Можно оставить пустым
            }
        });

        // Устанавливаем слушатель для ViewPager
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Ничего не делаем
            }

            @Override
            public void onPageSelected(int position) {
                Log.d("CategoryFragment", "Page selected: " + position); // Лог для отладки
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // Ничего не делаем
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}