package com.mgke.da.ui.stats;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.bumptech.glide.Glide;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.google.firebase.auth.FirebaseUser;
import com.mgke.da.models.Goal;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mgke.da.R;
import com.mgke.da.adapters.GoalAdapter;
import com.mgke.da.databinding.FragmentStatsBinding;
import com.mgke.da.models.Transaction;
import com.mgke.da.repository.GoalRepository;
import com.mgke.da.repository.TransactionRepository;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatsFragment extends Fragment implements GoalAdapter.OnGoalClickListener{
    private FragmentStatsBinding binding;
    private PieChart pieChart;
    private HorizontalBarChart horizontalBarChart;
    private RadarChart radarChart;
    private Calendar calendar;
    private int selectedTab;
    private TransactionRepository transactionRepository;
    private TabLayout tabLayout;
    private LinearLayout statisticsContainer, goalsContainer;
    private TextView processBtn, completedBtn;
    private RecyclerView recyclerViewGoals;
    private GoalAdapter goalAdapter;
    private List<Goal> goalList;
    private GoalRepository goalRepository;
    private String currentCurrency;

    public StatsFragment() {
    }
    @Override
    public void onGoalClick(Goal goal) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("goal", goal); // Передаем объект Goal
        NavHostFragment.findNavController(this).navigate(R.id.AddGoalFragment, bundle);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        pieChart = binding.pieChart;
        horizontalBarChart = binding.horizontalBarChart;
        radarChart = binding.radarChart;
        processBtn = binding.processBtn;
        completedBtn = binding.completedBtn;

        recyclerViewGoals = root.findViewById(R.id.recyclerViewGoals);
        goalList = new ArrayList<>();
        goalRepository = new GoalRepository(FirebaseFirestore.getInstance());
        transactionRepository = new TransactionRepository(FirebaseFirestore.getInstance());

        goalAdapter = new GoalAdapter(goalList, requireContext(), goalRepository,transactionRepository, currentCurrency, this);
        recyclerViewGoals.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewGoals.setAdapter(goalAdapter);

        loadGoals(false);

        setupPieChart();
        setupHorizontalBarChart();

        setSelectedButtonGoal(processBtn, completedBtn);

        tabLayout = binding.tabLayout;
        statisticsContainer = binding.statisticsContainer;
        goalsContainer = binding.goalsContainer;
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.stats)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.goals)));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        statisticsContainer.setVisibility(View.VISIBLE);
                        goalsContainer.setVisibility(View.GONE);
                        break;
                    case 1:
                        statisticsContainer.setVisibility(View.GONE);
                        goalsContainer.setVisibility(View.VISIBLE);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        statisticsContainer.setVisibility(View.VISIBLE);
        goalsContainer.setVisibility(View.GONE);
        radarChart.setVisibility(View.GONE);

        transactionRepository = new TransactionRepository(FirebaseFirestore.getInstance());
        calendar = Calendar.getInstance();
        selectedTab = 0;
        selectDayButton();
        setTransactionType("DOHOD");
        loadStatsData();
        updateDateText();

        binding.dayBtn.setOnClickListener(v -> {
            setSelectedButton(binding.dayBtn, binding.monthlyBtn, binding.monthsBtn);
            selectedTab = 0;
            updateDateText();
            loadStatsData();
            showCharts(true);
        });

        binding.monthlyBtn.setOnClickListener(v -> {
            setSelectedButton(binding.monthlyBtn, binding.dayBtn, binding.monthsBtn);
            selectedTab = 1;
            updateDateText();
            loadStatsData();
            showCharts(true);
        });

        binding.monthsBtn.setOnClickListener(v -> {
            setSelectedButton(binding.monthsBtn, binding.dayBtn, binding.monthlyBtn);
            selectedTab = 2;
            updateDateText();
            loadStatsData();
            showCharts(false);
        });

        binding.previousDate.setOnClickListener(v -> {
            updateCalendar(-1);
            loadStatsData();
        });

        binding.nextDate.setOnClickListener(v -> {
            updateCalendar(1);
            loadStatsData();
        });

        binding.incomeBtn.setOnClickListener(v -> {
            setTransactionType("DOHOD");
            loadStatsData();
        });

        binding.expenseBtn.setOnClickListener(v -> {
            setTransactionType("RACHOD");
            loadStatsData();
        });

        setupButtonListeners();
        setupGoalsButtonListeners();

        FloatingActionButton fabAddGoal = binding.fabAddGoal;
        fabAddGoal.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.AddGoalFragment);
        });

        return root;
    }

    private boolean isDarkTheme() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private void setupGoalsButtonListeners() {

        processBtn.setOnClickListener(v -> {
            setSelectedButtonGoal(processBtn, completedBtn);
            loadGoals(false);
        });

        completedBtn.setOnClickListener(v -> {
            setSelectedButtonGoal(completedBtn, processBtn);
            loadGoals(true);
        });
    }

    private void setSelectedButtonGoal(TextView selected, TextView unselected) {
        selected.setBackgroundResource(R.drawable.selector_goal);
        selected.setTextColor(isDarkTheme() ? Color.WHITE : Color.BLACK);
        unselected.setBackgroundResource(R.drawable.transaction_add_default_selector);
        unselected.setTextColor(isDarkTheme() ? Color.WHITE : Color.BLACK);
    }
    private void loadGoals(boolean showCompleted) {
        String currentUserId = getCurrentUserId(); // Метод для получения текущего userId

        goalRepository.getUserGoals(currentUserId).thenAccept(goals -> {
            goalList.clear();
            for (Goal goal : goals) {
                checkGoalCompletion(goal);
                if (showCompleted && goal.isCompleted) {
                    goalList.add(goal);
                } else if (!showCompleted && !goal.isCompleted) {
                    goalList.add(goal);
                }
            }

            goalAdapter.notifyDataSetChanged();

            if (goalList.isEmpty()) {
                binding.emptyStateImageView.setVisibility(View.VISIBLE);
                binding.emptyStateTextView2.setVisibility(View.VISIBLE);
                int gifResource = isDarkTheme() ? R.drawable.document_search_night : R.drawable.document_search;
                Glide.with(this)
                        .asGif()
                        .load(gifResource)
                        .into(binding.emptyStateImageView);
            } else {
                binding.emptyStateImageView.setVisibility(View.GONE);
                binding.emptyStateTextView2.setVisibility(View.GONE);
            }
        });
    }

    // Пример метода для получения текущего идентификатора пользователя
    private String getCurrentUserId() {
        // Предположим, что используете Firebase Auth для получения текущего пользователя
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    private void checkGoalCompletion(Goal goal) {
        if (goal.progress >= goal.targetAmount) {
            goal.isCompleted = true;
            goalRepository.updateGoal(goal);
        }
    }

    private void setupButtonListeners() {
        binding.dayBtn.setOnClickListener(v -> {
            setSelectedButton(binding.dayBtn, binding.monthlyBtn, binding.monthsBtn);
            selectedTab = 0;
            updateDateText();
            loadStatsData();
            showCharts(true);
        });

        binding.monthlyBtn.setOnClickListener(v -> {
            setSelectedButton(binding.monthlyBtn, binding.dayBtn, binding.monthsBtn);
            selectedTab = 1;
            updateDateText();
            loadStatsData();
            showCharts(true);
        });

        binding.monthsBtn.setOnClickListener(v -> {
            setSelectedButton(binding.monthsBtn, binding.dayBtn, binding.monthlyBtn);
            selectedTab = 2;
            updateDateText();
            loadStatsData();
            showCharts(false);
        });

        binding.previousDate.setOnClickListener(v -> {
            updateCalendar(-1);
            loadStatsData();
        });

        binding.nextDate.setOnClickListener(v -> {
            updateCalendar(1);
            loadStatsData();
        });

        binding.incomeBtn.setOnClickListener(v -> {
            setTransactionType("DOHOD");
            loadStatsData();
        });

        binding.expenseBtn.setOnClickListener(v -> {
            setTransactionType("RACHOD");
            loadStatsData();
        });
    }

    private void showCharts(boolean showStandardCharts) {
        if (showStandardCharts) {
            radarChart.setVisibility(View.GONE);
            pieChart.setVisibility(View.VISIBLE);
            horizontalBarChart.setVisibility(View.VISIBLE);
            binding.linearLayout.setVisibility(View.VISIBLE);
        } else {
            radarChart.setVisibility(View.VISIBLE);
            pieChart.setVisibility(View.GONE);
            horizontalBarChart.setVisibility(View.GONE);
            binding.linearLayout.setVisibility(View.GONE);
        }
    }

    private void setSelectedButton(TextView selected, TextView... unselected) {
        boolean isDarkTheme = (getActivity().getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        if (isDarkTheme) {
            selected.setBackgroundResource(R.drawable.day_month_selector_night);
            selected.setTextColor(Color.WHITE);
        } else {
            selected.setBackgroundResource(R.drawable.day_month_selector_active);
            selected.setTextColor(Color.WHITE);
        }

        for (TextView button : unselected) {
            if (isDarkTheme) {
                button.setBackgroundResource(R.drawable.day_month_selector_white);
                button.setTextColor(Color.WHITE);
            } else {
                button.setBackgroundResource(R.drawable.day_month_selector);
                button.setTextColor(Color.BLACK);
            }
        }
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setDrawCenterText(true);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);

        boolean isDarkTheme = (getActivity().getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        pieChart.setCenterTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
        pieChart.setEntryLabelColor(isDarkTheme ? Color.WHITE : Color.BLACK);
        pieChart.setEntryLabelTextSize(12f);

        Legend legend = pieChart.getLegend();
        legend.setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
        legend.setTextSize(12f);
        legend.setForm(Legend.LegendForm.CIRCLE);

        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            public void onValueSelected(Entry e, Highlight h) {
                if (e instanceof PieEntry) {
                    PieEntry pieEntry = (PieEntry) e;
                    String category = pieEntry.getLabel();
                    float value = pieEntry.getValue();
                    String message = getString(R.string.selected_category_message, category, value);
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected() {
            }
        });
    }

    private void setupHorizontalBarChart() {
        horizontalBarChart.getDescription().setEnabled(false);
        horizontalBarChart.setDrawGridBackground(false);
        horizontalBarChart.setFitBars(true);
        horizontalBarChart.setHighlightFullBarEnabled(false);
    }

    private void selectDayButton() {
        setSelectedButton(binding.dayBtn, binding.monthlyBtn);
        calendar = Calendar.getInstance();
        updateDateText();
    }

    private void updateDateText() {
        if (selectedTab == 0) {
            binding.currentDate.setText(formatDate(calendar.getTime()));
        } else {
            binding.currentDate.setText(formatMonth(calendar.getTime()));
        }
    }

    private void loadStatsData() {
        if (selectedTab == 0) {
            loadDailyTransactions();
        } else if (selectedTab == 1) {
            loadMonthlyTransactions();
        } else if (selectedTab == 2) {
            loadMonthlysTransactions();
        }
    }

    private void loadMonthlyTransactions() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Calendar startCalendar = Calendar.getInstance();
        Calendar endCalendar = Calendar.getInstance();
        startCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), 1, 0, 0, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);
        endCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), startCalendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        endCalendar.set(Calendar.MILLISECOND, 999);

        transactionRepository.getAllTransaction()
                .thenAccept(transactions -> {
                    Map<String, Float> categorySums = new HashMap<>();
                    for (Transaction transaction : transactions) {
                        boolean isIncomeActive = binding.incomeBtn.isSelected();
                        boolean isExpenseActive = binding.expenseBtn.isSelected();
                        if (transaction.userId.equals(currentUserId) &&
                                !transaction.date.before(startCalendar.getTime()) &&
                                !transaction.date.after(endCalendar.getTime())) {
                            String category = transaction.category;
                            float amount = (float) Math.abs(transaction.amount);

                            if ((transaction.type.equals("DOHOD") && isIncomeActive) ||
                                    (transaction.type.equals("RACHOD") && isExpenseActive)) {
                                categorySums.put(category, categorySums.getOrDefault(category, 0f) + amount);
                            }
                        }
                    }
                    updatePieChart(categorySums);
                    updateHorizontalBarChart(categorySums);
                })
                .exceptionally(e -> {
                    return null;
                });
    }

    private void loadDailyTransactions() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Calendar startCalendar = Calendar.getInstance();
        Calendar endCalendar = Calendar.getInstance();
        startCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), 0, 0, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);
        endCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), 23, 59, 59);
        endCalendar.set(Calendar.MILLISECOND, 999);

        transactionRepository.getAllTransaction()
                .thenAccept(transactions -> {
                    Map<String, Float> categorySums = new HashMap<>();
                    for (Transaction transaction : transactions) {
                        boolean isIncomeActive = binding.incomeBtn.isSelected();
                        boolean isExpenseActive = binding.expenseBtn.isSelected();

                        if (transaction.userId.equals(currentUserId) &&
                                !transaction.date.before(startCalendar.getTime()) &&
                                !transaction.date.after(endCalendar.getTime())) {
                            String category = transaction.category;
                            float amount = (float) Math.abs(transaction.amount);

                            if ((transaction.type.equals("DOHOD") && isIncomeActive) ||
                                    (transaction.type.equals("RACHOD") && isExpenseActive)) {
                                categorySums.put(category, categorySums.getOrDefault(category, 0f) + amount);
                            }
                        }
                    }
                    updatePieChart(categorySums);
                    updateHorizontalBarChart(categorySums);
                })
                .exceptionally(e -> {
                    return null;
                });
    }

    private void loadMonthlysTransactions() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Calendar endCalendar = Calendar.getInstance();
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.add(Calendar.MONTH, -3);

        transactionRepository.getAllTransaction()
                .thenAccept(transactions -> {
                    Map<String, Float[]> categorySums = new HashMap<>();
                    boolean isIncomeActive = binding.incomeBtn.isSelected();
                    boolean isExpenseActive = binding.expenseBtn.isSelected();

                    for (Transaction transaction : transactions) {
                        Calendar transactionCalendar = Calendar.getInstance();
                        transactionCalendar.setTime(transaction.date);

                        if (transaction.userId.equals(currentUserId) &&
                                transactionCalendar.after(startCalendar) &&
                                transactionCalendar.before(endCalendar)) {
                            String category = transaction.category;
                            float amount = (float) Math.abs(transaction.amount);

                            if ((transaction.type.equals("DOHOD") && isIncomeActive) ||
                                    (transaction.type.equals("RACHOD") && isExpenseActive)) {

                                int monthIndex = 2 - (endCalendar.get(Calendar.MONTH) - transactionCalendar.get(Calendar.MONTH));
                                if (monthIndex < 0) monthIndex += 12;

                                categorySums.putIfAbsent(category, new Float[3]);
                                Float[] sums = categorySums.get(category);

                                if (sums[monthIndex] == null) sums[monthIndex] = 0f;
                                sums[monthIndex] += amount;
                            }
                        }
                    }
                    updateRadarChart(categorySums);
                })
                .exceptionally(e -> {
                    return null;
                });
    }

    private void updateRadarChart(Map<String, Float[]> categorySums) {
        FragmentStatsBinding binding = FragmentStatsBinding.bind(getView());
        ImageView noDataImageView = binding.noDataImageView;
        TextView emptyStateTextView = binding.emptyStateTextView;

        if (categorySums.isEmpty()) {
            radarChart.setVisibility(View.GONE);
            noDataImageView.setVisibility(View.VISIBLE);
            emptyStateTextView.setVisibility(View.VISIBLE);
            Glide.with(this).load(R.drawable.no_data).into(noDataImageView);
            return;
        } else {
            radarChart.setVisibility(View.VISIBLE);
            noDataImageView.setVisibility(View.GONE);
            emptyStateTextView.setVisibility(View.GONE);
        }

        RadarData radarData = new RadarData();
        List<Integer> monthColors = Arrays.asList(
                ColorTemplate.COLORFUL_COLORS[0],
                ColorTemplate.COLORFUL_COLORS[1],
                ColorTemplate.COLORFUL_COLORS[2]
        );

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -2);
        boolean isDarkTheme = (getActivity().getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        for (int month = 0; month < 3; month++) {
            List<RadarEntry> entries = new ArrayList<>();
            for (Map.Entry<String, Float[]> entry : categorySums.entrySet()) {
                Float[] sums = entry.getValue();
                float value = (sums[month] != null) ? sums[month] : 0;
                entries.add(new RadarEntry(value));
            }

            String monthName = new SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.getTime());
            RadarDataSet dataSet = new RadarDataSet(entries, monthName);
            dataSet.setColor(monthColors.get(month));
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(monthColors.get(month));
            dataSet.setValueTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
            dataSet.setValueTextSize(16f);
            radarData.addDataSet(dataSet);
            calendar.add(Calendar.MONTH, 1);
        }

        radarChart.setData(radarData);
        List<String> categoryLabels = new ArrayList<>(categorySums.keySet());
        radarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(categoryLabels));
        radarChart.getDescription().setEnabled(false);
        radarChart.getYAxis().setDrawLabels(false);
        radarChart.getXAxis().setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
        radarChart.getLegend().setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
        radarChart.animateXY(1000, 1000);
        radarChart.invalidate();
    }

    private void updatePieChart(Map<String, Float> categorySums) {
        FragmentStatsBinding binding = FragmentStatsBinding.bind(getView());
        ImageView noDataImageView = binding.noDataImageView;
        TextView emptyStateTextView = binding.emptyStateTextView;
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : categorySums.entrySet()) {
            if (entry.getValue() > 0) {
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }
        }

        if (entries.isEmpty()) {
            pieChart.setVisibility(View.GONE);
            noDataImageView.setVisibility(View.VISIBLE);
            emptyStateTextView.setVisibility(View.VISIBLE);
            Glide.with(this).load(R.drawable.no_data).into(noDataImageView);
            return;
        } else {
            pieChart.setVisibility(View.VISIBLE);
            noDataImageView.setVisibility(View.GONE);
            emptyStateTextView.setVisibility(View.GONE);
        }

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setDrawCenterText(true);

        boolean isDarkTheme = (getActivity().getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        pieChart.setCenterTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setValueTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
        dataSet.setValueTextSize(16f);
        dataSet.setColors(getPieChartColors(entries));

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.invalidate();
        pieChart.animateY(1000, Easing.EaseInOutQuad);
    }

    private void updateHorizontalBarChart(Map<String, Float> categorySums) {
        FragmentStatsBinding binding = FragmentStatsBinding.bind(getView());
        ImageView noDataImageView = binding.noDataImageView;
        TextView emptyStateTextView = binding.emptyStateTextView;
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int index = 0;
        for (Map.Entry<String, Float> entry : categorySums.entrySet()) {
            if (entry.getValue() > 0) {
                entries.add(new BarEntry(index++, entry.getValue()));
                labels.add(entry.getKey());
            }
        }

        if (entries.isEmpty()) {
            horizontalBarChart.setVisibility(View.GONE);
            noDataImageView.setVisibility(View.VISIBLE);
            emptyStateTextView.setVisibility(View.VISIBLE);
            Glide.with(this).load(R.drawable.no_data).into(noDataImageView);
            return;
        } else {
            horizontalBarChart.setVisibility(View.VISIBLE);
            noDataImageView.setVisibility(View.GONE);
            emptyStateTextView.setVisibility(View.GONE);
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(getBarChartColors(entries));

        boolean isDarkTheme = (getActivity().getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        dataSet.setValueTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
        dataSet.setValueTextSize(12f);
        dataSet.setDrawValues(true);

        float barWidth = 0.25f;
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(barWidth);
        horizontalBarChart.setData(barData);

        XAxis xAxis = horizontalBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.TRANSPARENT);
        xAxis.setLabelCount(0, true);

        YAxis yAxisLeft = horizontalBarChart.getAxisLeft();
        yAxisLeft.setEnabled(false);
        yAxisLeft.setGranularity(0.2f);
        yAxisLeft.setLabelCount(5, true);
        yAxisLeft.setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
        yAxisLeft.setDrawGridLines(false);

        YAxis yAxisRight = horizontalBarChart.getAxisRight();
        yAxisRight.setEnabled(false);
        xAxis.setAxisMinimum(0f);
        horizontalBarChart.animateY(1000);
        horizontalBarChart.getLegend().setEnabled(false);
        horizontalBarChart.setDescription(null);
        horizontalBarChart.setDrawGridBackground(false);
        horizontalBarChart.setDrawBarShadow(false);
        horizontalBarChart.invalidate();
    }

    private List<Integer> getPieChartColors(List<PieEntry> entries) {
        List<Integer> colors = new ArrayList<>();
        int[] colorPalette = ColorTemplate.JOYFUL_COLORS;

        for (int i = 0; i < entries.size(); i++) {
            colors.add(colorPalette[i % colorPalette.length]);
        }
        return colors;
    }

    private List<Integer> getBarChartColors(List<BarEntry> entries) {
        List<Integer> colors = new ArrayList<>();
        int[] colorPalette = ColorTemplate.JOYFUL_COLORS;

        for (int i = 0; i < entries.size(); i++) {
            colors.add(colorPalette[i % colorPalette.length]);
        }
        return colors;
    }

    private void setTransactionType(String type) {
        boolean isDarkTheme = (getActivity().getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (type.equals("DOHOD")) {
            binding.incomeBtn.setBackgroundResource(R.drawable.transaction_add_income_selector);
            binding.expenseBtn.setBackgroundResource(R.drawable.transaction_add_default_selector);
            binding.incomeBtn.setTextColor(isDarkTheme ? Color.WHITE : Color.parseColor("#00C853"));
            binding.expenseBtn.setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
            binding.incomeBtn.setSelected(true);
            binding.expenseBtn.setSelected(false);
        } else if (type.equals("RACHOD")) {
            binding.incomeBtn.setBackgroundResource(R.drawable.transaction_add_default_selector);
            binding.expenseBtn.setBackgroundResource(R.drawable.transaction_add_expence_selector);
            binding.incomeBtn.setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
            binding.expenseBtn.setTextColor(isDarkTheme ? Color.WHITE : Color.RED);
            binding.incomeBtn.setSelected(false);
            binding.expenseBtn.setSelected(true);
        }
    }

    public static String formatDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault());
        return dateFormat.format(date);
    }

    public static String formatMonth(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault());
        return dateFormat.format(date);
    }

    private void updateCalendar(int increment) {
        if (selectedTab == 0) {
            calendar.add(Calendar.DATE, increment);
        } else {
            calendar.add(Calendar.MONTH, increment);
        }
        updateDateText();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}