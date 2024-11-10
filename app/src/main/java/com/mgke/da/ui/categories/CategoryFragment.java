package com.mgke.da.ui.categories;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.databinding.FragmentCategoryBinding;
import com.mgke.da.repository.CategoryRepository;

public class CategoryFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private CategoryRepository categoryRepository;
    private FirebaseUser currentUser;
    private String userId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category, container, false);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userId = currentUser != null ? currentUser.getUid() : null;

        categoryRepository = new CategoryRepository(FirebaseFirestore.getInstance());

        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        setupTabs();

        return view;
    }

    private void setupTabs() {
        CategoryPagerAdapter adapter = new CategoryPagerAdapter(getChildFragmentManager());
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    public class CategoryPagerAdapter extends FragmentPagerAdapter {

        public CategoryPagerAdapter(@NonNull FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return new ExpensesFragment(); // Загружаем фрагмент расходов
            } else {
                return new IncomeFragment(); // Загружаем фрагмент доходов
            }
        }

        @Override
        public int getCount() {
            return 2; // Два таба: расходы и доходы
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return position == 0 ? "Расходы" : "Доходы";
        }
    }
}

