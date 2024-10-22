package com.mgke.da.ui.articles;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.mgke.da.R;
import com.mgke.da.databinding.FragmentArticlesBinding;

public class ArticlesFragment extends Fragment {

    private FragmentArticlesBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentArticlesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        binding.addArticlesButton.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.fragment_add_articles)
        );

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
