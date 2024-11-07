package com.mgke.da.ui.categories;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import com.mgke.da.R;
import com.mgke.da.databinding.FragmentCategoryBinding;

public class CategoryFragment extends Fragment {

    private FragmentCategoryBinding binding;
    private TabLayout tabLayout;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        Log.d("CategoryFragment", "onCreateView called");
        binding = FragmentCategoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        setupViewPagerAndTabs();
        return root;
    }

    private void setupViewPagerAndTabs() {
        Log.d("CategoryFragment", "setupViewPagerAndTabs called");

        tabLayout = binding.tabLayout;
        ViewPager viewPager = binding.viewPager;

        viewPager.setAdapter(new FragmentStatePagerAdapter(getChildFragmentManager()) {
            @Override
            public int getCount() {
                return 2; // 2 вкладки: расходы и доходы
            }

            @Override
            public Fragment getItem(int position) {
                Log.d("CategoryFragment", "getItem called, position: " + position);
                if (position == 0) {
                    return new ExpensesFragment(); // Фрагмент для расходов
                } else {
                    return new IncomeFragment(); // Фрагмент для доходов
                }
            }

            @Override
            public CharSequence getPageTitle(int position) {
                if (position == 0) {
                    return getString(R.string.tab_expenses); // Заголовок для расходов
                } else {
                    return getString(R.string.tab_income); // Заголовок для доходов
                }
            }
        });

        tabLayout.setupWithViewPager(viewPager);
        viewPager.setCurrentItem(0); // По умолчанию показываем вкладку расходов
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d("CategoryFragment", "onResume called");
        setupViewPagerAndTabs(); // Обновление ViewPager при возврате
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("CategoryFragment", "onPause called");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("CategoryFragment", "onDestroyView called");
        binding = null;
    }
}
