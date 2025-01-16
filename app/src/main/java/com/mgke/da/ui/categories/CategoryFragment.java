package com.mgke.da.ui.categories;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.databinding.FragmentCategoryBinding;
import com.mgke.da.repository.CategoryRepository;

public class CategoryFragment extends Fragment {

    private FragmentCategoryBinding binding;
    private CategoryRepository categoryRepository;
    private FirebaseUser currentUser;
    private String userId;
    private ImageView backButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d("CategoryFragment", "onCreateView called");
        binding = FragmentCategoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        backButton = binding.backButton;

        backButton.setOnClickListener(v -> {
            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
            navController.popBackStack();
        });

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userId = currentUser != null ? currentUser.getUid() : null;
        categoryRepository = new CategoryRepository(FirebaseFirestore.getInstance());

        setupViewPagerAndTabs();
        return root;
    }

    private void setupViewPagerAndTabs() {
        Log.d("CategoryFragment", "setupViewPagerAndTabs called");

        // Настройка адаптера для ViewPager
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(new CategoryPagerAdapter(getChildFragmentManager()));
        binding.tabLayout.setupWithViewPager(viewPager);
        viewPager.setCurrentItem(0); // По умолчанию показываем вкладку расходов

        // Обработка событий переключения вкладок
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.d("CategoryFragment", "Tab selected: " + tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                Log.d("CategoryFragment", "Page selected: " + position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });
    }

    // Адаптер для ViewPager
    private class CategoryPagerAdapter extends FragmentStatePagerAdapter {

        public CategoryPagerAdapter(@NonNull FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            Log.d("CategoryPagerAdapter", "getItem called, position: " + position);
            return position == 0 ? new ExpensesFragment() : new IncomeFragment();
        }

        @Override
        public int getCount() {
            return 2; // Два таба: расходы и доходы
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return position == 0 ? getString(R.string.tab_expenses) : getString(R.string.tab_income);
        }
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
