package com.mgke.da.ui.categories;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
<<<<<<< HEAD
=======
import androidx.annotation.Nullable;
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
<<<<<<< HEAD
import com.mgke.da.R;
=======
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
import com.mgke.da.databinding.FragmentCategoryBinding;

public class CategoryFragment extends Fragment {

    private FragmentCategoryBinding binding;
<<<<<<< HEAD
=======
    private View tabIndicator; // Индикатор для вкладок
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
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

<<<<<<< HEAD
=======
        // Инициализация индикатора
        tabIndicator = binding.tabIndicator;

>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
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
<<<<<<< HEAD
                if (position == 0) {
                    return getString(R.string.tab_expenses); // Название вкладки "Расходы"
                } else {
                    return getString(R.string.tab_income); // Название вкладки "Доходы"
                }
            }
        });

        tabLayout.setupWithViewPager(viewPager);

        viewPager.setCurrentItem(0); // 0 - индекс вкладки "Расходы"
=======
                return position == 0 ? "Расходы" : "Доходы"; // Название вкладок
            }
        });

        // Связываем TabLayout с ViewPager
        tabLayout.setupWithViewPager(viewPager);

        // Устанавливаем выбранную вкладку по умолчанию
        viewPager.setCurrentItem(0); // 0 - индекс вкладки "Расходы"
        updateIndicatorPosition(0); // Устанавливаем начальную позицию индикатора
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee

        // Добавляем слушатель для переключения вкладок
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                Log.d("CategoryFragment", "Tab selected: " + position); // Лог для отладки
<<<<<<< HEAD
=======
                updateIndicatorPosition(position);
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
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

<<<<<<< HEAD
        // Устанавливаем слушатель для ViewPager
=======
        // Устанавливаем позицию индикатора при инициализации
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // Ничего не делаем
            }

            @Override
            public void onPageSelected(int position) {
                Log.d("CategoryFragment", "Page selected: " + position); // Лог для отладки
<<<<<<< HEAD
=======
                updateIndicatorPosition(position); // Обновляем индикатор при смене страницы
>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // Ничего не делаем
            }
        });
    }

<<<<<<< HEAD
=======
    private void updateIndicatorPosition(int position) {
        // Получаем выбранную вкладку
        View tabView = tabLayout.getTabAt(position).view; // Получаем представление вкладки

        // Устанавливаем позицию индикатора
        tabIndicator.setX(tabView.getX() + (tabView.getWidth() - tabIndicator.getWidth()) / 2);
        tabIndicator.setVisibility(View.VISIBLE);
    }

>>>>>>> 9f8e75c219182397181d8bbc885a00651fa3edee
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}