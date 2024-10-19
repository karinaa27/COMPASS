package com.mgke.da.ui.stats;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
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

public class StatsFragment extends Fragment {
    private FragmentStatsBinding binding;
    private PieChart pieChart;
    private HorizontalBarChart horizontalBarChart;// Добавлен HorizontalBarChart
    private RadarChart radarChart;
    private Calendar calendar;
    private int selectedTab; // 0 - Day, 1 - Month
    private TransactionRepository transactionRepository;
    private TabLayout tabLayout;
    private LinearLayout statisticsContainer, goalsContainer;
    private TextView processBtn, completedBtn;
    private RecyclerView recyclerViewGoals;
    private GoalAdapter goalAdapter;
    private List<Goal> goalList;
    private GoalRepository goalRepository;

    public StatsFragment() {
        // Required empty public constructor
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        pieChart = binding.pieChart;
        horizontalBarChart = binding.horizontalBarChart;
        radarChart = binding.radarChart;
        processBtn=binding.processBtn;
        completedBtn=binding.completedBtn;

        recyclerViewGoals = root.findViewById(R.id.recyclerViewGoals);
        goalList = new ArrayList<>();
        goalAdapter = new GoalAdapter(goalList);

        recyclerViewGoals.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewGoals.setAdapter(goalAdapter);

        goalRepository = new GoalRepository(FirebaseFirestore.getInstance());
        loadGoals(); // Метод для загрузки целей из базы данных


        setupPieChart();
        setupHorizontalBarChart();

        tabLayout = binding.tabLayout; // Изменено на binding
        statisticsContainer = binding.statisticsContainer;
        goalsContainer = binding.goalsContainer;
// Добавляем табы
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.stats)));
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.goals)));


        // Установите слушатель для обработки нажатий на табы
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        // Показываем элементы статистики и скрываем цели
                        statisticsContainer.setVisibility(View.VISIBLE);
                        goalsContainer.setVisibility(View.GONE);
                        break;
                    case 1:
                        // Показываем цели и скрываем элементы статистики
                        statisticsContainer.setVisibility(View.GONE);
                        goalsContainer.setVisibility(View.VISIBLE);
                        break;
                }
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

        // Изначально показываем статистику
        statisticsContainer.setVisibility(View.VISIBLE);
        goalsContainer.setVisibility(View.GONE);

        // Скрыть RadarChart по умолчанию
        radarChart.setVisibility(View.GONE);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        transactionRepository = new TransactionRepository(FirebaseFirestore.getInstance());
        calendar = Calendar.getInstance();
        selectedTab = 0; // Устанавливаем начальный таб
        selectDayButton();
        setTransactionType("DOHOD");
        loadStatsData();
        updateDateText();

        binding.dayBtn.setOnClickListener(v -> {
            setSelectedButton(binding.dayBtn, binding.monthlyBtn, binding.monthsBtn);
            selectedTab = 0;
            updateDateText();
            loadStatsData();
            showCharts(true); // Показать PieChart и HorizontalBarChart
        });
        binding.monthlyBtn.setOnClickListener(v -> {
            setSelectedButton(binding.monthlyBtn, binding.dayBtn, binding.monthsBtn);
            selectedTab = 1;
            updateDateText();
            loadStatsData();
            showCharts(true); // Показать PieChart и HorizontalBarChart
        });
        binding.monthsBtn.setOnClickListener(v -> {
            setSelectedButton(binding.monthsBtn, binding.dayBtn, binding.monthlyBtn);
            selectedTab = 2;
            updateDateText();
            loadStatsData();
            showCharts(false); // Показать только RadarChart
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

        // Инициализация FloatingActionButton
        FloatingActionButton fabAddGoal = binding.fabAddGoal;
        fabAddGoal.setOnClickListener(v -> {
            // Переход к фрагменту добавления целей
            NavHostFragment.findNavController(this).navigate(R.id.AddGoalFragment);
        });
        return root;
    }

    private void setupGoalsButtonListeners() {
        TextView processBtn = binding.processBtn; // Инициализация кнопки "В процессе"
        TextView completedBtn = binding.completedBtn; // Инициализация кнопки "Завершенные"

        processBtn.setOnClickListener(v -> {
            setSelectedButtonGoal(processBtn, completedBtn);
            // Логика для обработки нажатия на "В процессе"
        });

        completedBtn.setOnClickListener(v -> {
            setSelectedButtonGoal(completedBtn, processBtn);
            // Логика для обработки нажатия на "Завершенные"
        });
    }

    private void setSelectedButtonGoal(TextView selected, TextView unselected) {
        selected.setBackgroundResource(R.drawable.transaction_add_income_selector); // Установите активный фон
        selected.setTextColor(Color.WHITE); // Измените цвет текста для выбранной кнопки

        unselected.setBackgroundResource(R.drawable.transaction_add_default_selector); // Установите фон для невыбранной кнопки
        unselected.setTextColor(Color.BLACK); // Цвет текста для невыбранной кнопки
    }


    private void setupButtonListeners() {
        binding.dayBtn.setOnClickListener(v -> {
            setSelectedButton(binding.dayBtn, binding.monthlyBtn, binding.monthsBtn);
            selectedTab = 0;
            updateDateText();
            loadStatsData();
            showCharts(true); // Показать PieChart и HorizontalBarChart
        });

        binding.monthlyBtn.setOnClickListener(v -> {
            setSelectedButton(binding.monthlyBtn, binding.dayBtn, binding.monthsBtn);
            selectedTab = 1;
            updateDateText();
            loadStatsData();
            showCharts(true); // Показать PieChart и HorizontalBarChart
        });

        binding.monthsBtn.setOnClickListener(v -> {
            setSelectedButton(binding.monthsBtn, binding.dayBtn, binding.monthlyBtn);
            selectedTab = 2;
            updateDateText();
            loadStatsData();
            showCharts(false); // Показать только RadarChart
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
            binding.linearLayout.setVisibility(View.VISIBLE); // Показать LinearLayout
        } else {
            radarChart.setVisibility(View.VISIBLE);
            pieChart.setVisibility(View.GONE);
            horizontalBarChart.setVisibility(View.GONE);
            binding.linearLayout.setVisibility(View.GONE); // Скрыть LinearLayout
        }
    }

    private void setSelectedButton(TextView selected, TextView... unselected) {
        boolean isDarkTheme = (getActivity().getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        // Установка фона и цвета текста для выбранной кнопки
        if (isDarkTheme) {
            selected.setBackgroundResource(R.drawable.day_month_selector_night);
            selected.setTextColor(Color.WHITE);
        } else {
            selected.setBackgroundResource(R.drawable.day_month_selector_active);
            selected.setTextColor(Color.WHITE);
        }

        // Установка фона и цвета текста для невыбранных кнопок
        for (TextView button : unselected) {
            if (isDarkTheme) {
                button.setBackgroundResource(R.drawable.day_month_selector_white);
                button.setTextColor(Color.WHITE);
            } else {
                button.setBackgroundResource(R.drawable.day_month_selector);
                button.setTextColor(Color.BLACK); // Установите цвет текста для невыбранных кнопок
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

        // Устанавливаем цвет текста в зависимости от темы
        boolean isDarkTheme = (getActivity().getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        // Цвет текста для центра
        pieChart.setCenterTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);

        // Настраиваем цвет меток (названий категорий)
        pieChart.setEntryLabelColor(isDarkTheme ? Color.WHITE : Color.BLACK);  // Цвет названий категорий
        pieChart.setEntryLabelTextSize(12f);  // Можно настроить размер текста

        // Настройка легенды (метки под диаграммой)
        Legend legend = pieChart.getLegend();
        legend.setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);  // Цвет текста легенды
        legend.setTextSize(12f);  // Размер текста легенды
        legend.setForm(Legend.LegendForm.CIRCLE);  // Форма значков легенды (можно использовать другие: LINE, SQUARE)

        pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                if (e instanceof PieEntry) {
                    PieEntry pieEntry = (PieEntry) e;
                    String category = pieEntry.getLabel();
                    float value = pieEntry.getValue();
                    Toast.makeText(getContext(), "Выбрана категория: " + category + ", сумма: " + value, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected() {
                // Действия при отсутствии выбора
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
        calendar = Calendar.getInstance(); // Сброс на текущую дату
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
            loadDailyTransactions(); // Загрузка транзакций за день
        } else if (selectedTab == 1) {
            loadMonthlyTransactions(); // Загрузка транзакций за месяц
        } else if(selectedTab == 2)
        {
            loadMonthlysTransactions();
        }
    }

    private void loadMonthlyTransactions() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.e("StatsFragment", "Пользователь не аутентифицирован");
            return;
        }

        Calendar startCalendar = Calendar.getInstance();
        Calendar endCalendar = Calendar.getInstance();
        startCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), 1, 0, 0, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);
        endCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), startCalendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        endCalendar.set(Calendar.MILLISECOND, 999);

        Log.d("StatsFragment", "Start Date: " + startCalendar.getTime());
        Log.d("StatsFragment", "End Date: " + endCalendar.getTime());

        transactionRepository.getAllTransaction()
                .thenAccept(transactions -> {
                    Log.d("StatsFragment", "Полученные транзакции: " + transactions.size());
                    Map<String, Float> categorySums = new HashMap<>();

                    for (Transaction transaction : transactions) {
                        Log.d("StatsFragment", "Обработка транзакции: " + transaction.toString());
                        Log.d("StatsFragment", "Тип транзакции: " + transaction.type);
                        Log.d("StatsFragment", "Дата транзакции: " + transaction.date);
                        Log.d("StatsFragment", "Сумма транзакции: " + transaction.amount);

                        // Проверка состояния кнопок
                        boolean isIncomeActive = binding.incomeBtn.isSelected();
                        boolean isExpenseActive = binding.expenseBtn.isSelected();
                        Log.d("StatsFragment", "Кнопка DOHOD активна: " + isIncomeActive);
                        Log.d("StatsFragment", "Кнопка RACHOD активна: " + isExpenseActive);

                        // Проверка диапазона дат
                        if (!transaction.date.before(startCalendar.getTime()) && !transaction.date.after(endCalendar.getTime())) {
                            Log.d("StatsFragment", "Транзакция в диапазоне дат");
                            String category = transaction.category;
                            float amount = (float) Math.abs(transaction.amount);

                            // Фильтрация по типу транзакции
                            if ((transaction.type.equals("DOHOD") && isIncomeActive) ||
                                    (transaction.type.equals("RACHOD") && isExpenseActive)) {
                                categorySums.put(category, categorySums.getOrDefault(category, 0f) + amount);
                            }
                        } else {
                            Log.d("StatsFragment", "Транзакция вне диапазона дат");
                        }
                    }

                    Log.d("StatsFragment", "Суммы по категориям после обработки: " + categorySums);
                    updatePieChart(categorySums);
                    updateHorizontalBarChart(categorySums); // Обновление HorizontalBarChart
                })
                .exceptionally(e -> {
                    Log.e("StatsFragment", "Ошибка при получении транзакций: " + e.getMessage());
                    return null;
                });
    }

    private void loadDailyTransactions() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.e("StatsFragment", "Пользователь не аутентифицирован");
            return;
        }

        Calendar startCalendar = Calendar.getInstance();
        Calendar endCalendar = Calendar.getInstance();
        startCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), 0, 0, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);
        endCalendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), 23, 59, 59);
        endCalendar.set(Calendar.MILLISECOND, 999);

        Log.d("StatsFragment", "Start Date: " + startCalendar.getTime());
        Log.d("StatsFragment", "End Date: " + endCalendar.getTime());

        transactionRepository.getAllTransaction()
                .thenAccept(transactions -> {
                    Log.d("StatsFragment", "Полученные транзакции: " + transactions.size());
                    Map<String, Float> categorySums = new HashMap<>();

                    for (Transaction transaction : transactions) {
                        Log.d("StatsFragment", "Обработка транзакции: " + transaction.toString());
                        Log.d("StatsFragment", "Тип транзакции: " + transaction.type);
                        Log.d("StatsFragment", "Дата транзакции: " + transaction.date);
                        Log.d("StatsFragment", "Сумма транзакции: " + transaction.amount);

                        // Проверка состояния кнопок
                        boolean isIncomeActive = binding.incomeBtn.isSelected();
                        boolean isExpenseActive = binding.expenseBtn.isSelected();
                        Log.d("StatsFragment", "Кнопка DOHOD активна: " + isIncomeActive);
                        Log.d("StatsFragment", "Кнопка RACHOD активна: " + isExpenseActive);

                        // Проверка диапазона дат
                        if (!transaction.date.before(startCalendar.getTime()) && !transaction.date.after(endCalendar.getTime())) {
                            Log.d("StatsFragment", "Транзакция в диапазоне дат");
                            String category = transaction.category;
                            float amount = (float) Math.abs(transaction.amount);

                            // Фильтрация по типу транзакции
                            if ((transaction.type.equals("DOHOD") && isIncomeActive) ||
                                    (transaction.type.equals("RACHOD") && isExpenseActive)) {
                                categorySums.put(category, categorySums.getOrDefault(category, 0f) + amount);
                            }
                        } else {
                            Log.d("StatsFragment", "Транзакция вне диапазона дат");
                        }
                    }

                    Log.d("StatsFragment", "Суммы по категориям после обработки: " + categorySums);
                    updatePieChart(categorySums);
                    updateHorizontalBarChart(categorySums); // Обновление HorizontalBarChart
                })
                .exceptionally(e -> {
                    Log.e("StatsFragment", "Ошибка при получении транзакций: " + e.getMessage());
                    return null;
                });
    }

    private void loadMonthlysTransactions() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.e("StatsFragment", "Пользователь не аутентифицирован");
            return;
        }

        Calendar endCalendar = Calendar.getInstance();
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.add(Calendar.MONTH, -2); // Начальная дата - 2 месяца назад

        transactionRepository.getAllTransaction()
                .thenAccept(transactions -> {
                    Log.d("StatsFragment", "Полученные транзакции: " + transactions.size());
                    Map<String, Float[]> categorySums = new HashMap<>();

                    // Проверяем состояние кнопок
                    boolean isIncomeActive = binding.incomeBtn.isSelected();
                    boolean isExpenseActive = binding.expenseBtn.isSelected();
                    Log.d("StatsFragment", "Кнопка DOHOD активна: " + isIncomeActive);
                    Log.d("StatsFragment", "Кнопка RACHOD активна: " + isExpenseActive);

                    // Инициализируем массив для трех месяцев
                    for (Transaction transaction : transactions) {
                        Calendar transactionCalendar = Calendar.getInstance();
                        transactionCalendar.setTime(transaction.date);

                        // Проверяем, попадает ли транзакция в диапазон последних 3 месяцев
                        if (transactionCalendar.after(startCalendar) && transactionCalendar.before(endCalendar)) {
                            String category = transaction.category;
                            float amount = (float) Math.abs(transaction.amount); // Берем абсолютное значение суммы

                            // Фильтрация по типу транзакции
                            if ((transaction.type.equals("DOHOD") && isIncomeActive) ||
                                    (transaction.type.equals("RACHOD") && isExpenseActive)) {

                                int monthIndex = 2 - (endCalendar.get(Calendar.MONTH) - transactionCalendar.get(Calendar.MONTH));
                                if (monthIndex < 0) monthIndex += 12; // Обработка перехода через год

                                // Инициализация массива для категории, если он еще не создан
                                categorySums.putIfAbsent(category, new Float[3]);
                                Float[] sums = categorySums.get(category);

                                // Увеличиваем сумму за соответствующий месяц
                                if (sums[monthIndex] == null) sums[monthIndex] = 0f;
                                sums[monthIndex] += amount;
                            }
                        }
                    }

                    Log.d("StatsFragment", "Суммы по категориям за последние три месяца: " + categorySums);
                    updateRadarChart(categorySums); // Обновляем RadarChart
                })
                .exceptionally(e -> {
                    Log.e("StatsFragment", "Ошибка при получении транзакций: " + e.getMessage());
                    return null;
                });
    }

    private void updateRadarChart(Map<String, Float[]> categorySums) {
        // Получаем ссылку на TextView для отображения сообщения через View Binding
        FragmentStatsBinding binding = FragmentStatsBinding.bind(getView()); // Получаем биндинг для текущего представления
        TextView noDataTextView = binding.noDataTextView; // Используем биндинг для доступа к TextView

        // Проверяем, есть ли данные для отображения
        if (categorySums.isEmpty()) {
            radarChart.setVisibility(View.GONE); // Скрываем график
            noDataTextView.setVisibility(View.VISIBLE); // Показываем сообщение о отсутствии данных
            return; // Прекращаем выполнение метода
        } else {
            radarChart.setVisibility(View.VISIBLE); // Показываем график, если есть данные
            noDataTextView.setVisibility(View.GONE); // Скрываем сообщение, если есть данные
        }

        // Создаем объект RadarData
        RadarData radarData = new RadarData();

        // Определяем цвета для трех месяцев
        List<Integer> monthColors = Arrays.asList(
                ColorTemplate.COLORFUL_COLORS[0], // Цвет для первого месяца
                ColorTemplate.COLORFUL_COLORS[1], // Цвет для второго месяца
                ColorTemplate.COLORFUL_COLORS[2]  // Цвет для третьего месяца
        );

        // Получаем текущую дату для определения названий месяцев
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -2); // Устанавливаем календарь на два месяца назад

        // Определяем, темная ли тема
        boolean isDarkTheme = (getActivity().getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        // Создаем список для хранения RadarDataSet для каждого месяца
        for (int month = 0; month < 3; month++) {
            List<RadarEntry> entries = new ArrayList<>();

            // Проходим по категориям и создаем RadarEntry для каждого месяца
            for (Map.Entry<String, Float[]> entry : categorySums.entrySet()) {
                String category = entry.getKey();
                Float[] sums = entry.getValue();

                // Если сумма за месяц не null, добавляем RadarEntry
                float value = (sums[month] != null) ? sums[month] : 0;
                entries.add(new RadarEntry(value));
            }

            // Форматируем название месяца
            String monthName = new SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.getTime());

            // Создаем новый набор данных для текущего месяца
            RadarDataSet dataSet = new RadarDataSet(entries, monthName);
            dataSet.setColor(monthColors.get(month)); // Устанавливаем цвет для месяца
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(monthColors.get(month));
            dataSet.setValueTextColor(isDarkTheme ? Color.WHITE : Color.BLACK); // Цвет текста значений
            dataSet.setValueTextSize(16f);

            // Добавляем набор данных в RadarData
            radarData.addDataSet(dataSet);

            // Переходим к следующему месяцу
            calendar.add(Calendar.MONTH, 1);
        }

        // Устанавливаем данные в RadarChart
        radarChart.setData(radarData);

        // Установка меток категорий на осях
        List<String> categoryLabels = new ArrayList<>(categorySums.keySet());
        radarChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(categoryLabels));

        // Убираем описание и числа на осях
        radarChart.getDescription().setEnabled(false); // Отключаем описание графика
        radarChart.getYAxis().setDrawLabels(false); // Убираем метки на оси Y

        // Устанавливаем цвет меток категорий в зависимости от темы
        radarChart.getXAxis().setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK); // Цвет текста меток

        // Установка цвета текста в легенде в зависимости от темы
        radarChart.getLegend().setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK); // Цвет текста в легенде

        // Настройка анимации
        radarChart.animateXY(1000, 1000); // Анимация появления графика

        radarChart.invalidate(); // Обновление графика
    }



    private void updatePieChart(Map<String, Float> categorySums) {
        List<PieEntry> entries = new ArrayList<>();

        for (Map.Entry<String, Float> entry : categorySums.entrySet()) {
            if (entry.getValue() > 0) { // Добавляем только ненулевые значения
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }
        }

        if (entries.isEmpty()) {
            Toast.makeText(getContext(), "Нет данных для отображения", Toast.LENGTH_SHORT).show();
            pieChart.clear(); // Очистка графика
            return;
        }

        // Устанавливаем настройки для PieChart
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setDrawCenterText(true);

        // Проверка темы
        boolean isDarkTheme = (getActivity().getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        // Устанавливаем цвет текста для центра (пояснительная надпись)
        pieChart.setCenterTextColor(isDarkTheme ? Color.WHITE : Color.BLACK); // Цвет текста центра

        // Создаем PieDataSet
        PieDataSet dataSet = new PieDataSet(entries, "");  // Пустая строка вместо заголовка

        // Устанавливаем цвет текста значений
        dataSet.setValueTextColor(isDarkTheme ? Color.WHITE : Color.BLACK); // Цвет текста значений
        dataSet.setValueTextSize(16f); // Размер текста значений

        // Устанавливаем цвета для секторов
        dataSet.setColors(getPieChartColors(entries));

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.invalidate(); // Обновление графика

        // Добавляем анимацию
        pieChart.animateY(1000, Easing.EaseInOutQuad); // Анимация по оси Y за 1000 мс
    }


    private void updateHorizontalBarChart(Map<String, Float> categorySums) {
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
            Toast.makeText(getContext(), "Нет данных для отображения", Toast.LENGTH_SHORT).show();
            horizontalBarChart.clear(); // Очистка графика
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColors(getBarChartColors(entries));

        // Цвет текста значений в зависимости от темы
        boolean isDarkTheme = (getActivity().getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        dataSet.setValueTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
        dataSet.setValueTextSize(12f);
        dataSet.setDrawValues(true);

        float barWidth = 0.25f; // Ширина баров
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(barWidth);

        horizontalBarChart.setData(barData);

        // Настройка осей
        XAxis xAxis = horizontalBarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawAxisLine(false); // Отключаем осевую линию
        xAxis.setDrawGridLines(false); // Отключаем сетку
        xAxis.setTextColor(Color.TRANSPARENT); // Убираем цвет текста меток (делаем прозрачными)
        xAxis.setLabelCount(0, true); // Отключаем метки

        // Настройка оси Y
        YAxis yAxisLeft = horizontalBarChart.getAxisLeft();
        yAxisLeft.setEnabled(false);
        yAxisLeft.setGranularity(0.2f);
        yAxisLeft.setLabelCount(5, true);
        yAxisLeft.setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK);
        yAxisLeft.setDrawGridLines(false);

        YAxis yAxisRight = horizontalBarChart.getAxisRight();
        yAxisRight.setEnabled(false);

        // Установка минимального значения для оси X
        xAxis.setAxisMinimum(0f); // Установка минимального значения оси X

        // Установка анимации
        horizontalBarChart.animateY(1000);

        // Настройки оформления графика
        horizontalBarChart.getLegend().setEnabled(false);
        horizontalBarChart.setDescription(null);
        horizontalBarChart.setDrawGridBackground(false);
        horizontalBarChart.setDrawBarShadow(false);

        // Обновляем график
        horizontalBarChart.invalidate();
    }

    private List<Integer> getPieChartColors(List<PieEntry> entries) {
        List<Integer> colors = new ArrayList<>();
        int[] colorPalette = ColorTemplate.JOYFUL_COLORS; // Массив цветов

        for (int i = 0; i < entries.size(); i++) {
            colors.add(colorPalette[i % colorPalette.length]); // Циклическое использование цветов
        }
        return colors;
    }

    private List<Integer> getBarChartColors(List<BarEntry> entries) {
        List<Integer> colors = new ArrayList<>();
        int[] colorPalette = ColorTemplate.JOYFUL_COLORS; // Массив цветов

        for (int i = 0; i < entries.size(); i++) {
            colors.add(colorPalette[i % colorPalette.length]); // Циклическое использование цветов
        }
        return colors;
    }

    private void loadGoals() {
        goalRepository.getAllGoal().thenAccept(goals -> {
            goalList.clear();
            goalList.addAll(goals);
            goalAdapter.notifyDataSetChanged();
        });
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
            // Устанавливаем цвет текста в зависимости от темы
            binding.incomeBtn.setTextColor(isDarkTheme ? Color.WHITE : Color.BLACK); // Черный для дохода в светлой теме
            binding.expenseBtn.setTextColor(isDarkTheme ? Color.WHITE : Color.RED); // Красный для расхода в светлой теме

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