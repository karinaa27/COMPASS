    package com.mgke.da.ui.transactions;

    import android.content.Context;
    import android.content.SharedPreferences;
    import android.content.res.Configuration;
    import android.os.Bundle;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.Button;
    import android.widget.ImageView;
    import android.widget.TextView;
    import androidx.annotation.NonNull;
    import androidx.fragment.app.Fragment;
    import androidx.navigation.NavController;
    import androidx.navigation.Navigation;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import com.bumptech.glide.Glide;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.firestore.FirebaseFirestore;
    import com.google.firebase.firestore.ListenerRegistration;
    import com.google.firebase.firestore.Query;
    import com.google.firebase.firestore.QueryDocumentSnapshot;
    import com.mgke.da.Constants;
    import com.mgke.da.R;
    import com.mgke.da.adapters.TransactionAdapter;
    import com.mgke.da.databinding.FragmentTransactionsBinding;
    import com.mgke.da.models.PersonalData;
    import com.mgke.da.models.Transaction;
    import com.mgke.da.repository.AccountRepository;
    import com.mgke.da.repository.PersonalDataRepository;
    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.Calendar;
    import java.util.Date;
    import java.util.List;
    import java.util.Locale;

    public class TransactionsFragment extends Fragment {

        private FragmentTransactionsBinding binding;
        private Calendar calendar;
        private TransactionAdapter adapter;
        private List<Transaction> transactionList;
        private ImageView emptyStateImageView;
        private PersonalData personalData;
        private TextView emptyStateTextView;
        private ListenerRegistration transactionsListener;
        private PersonalDataRepository personalDataRepository;

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 ViewGroup container, Bundle savedInstanceState) {
            binding = FragmentTransactionsBinding.inflate(inflater, container, false);
            View root = binding.getRoot();

            transactionList = new ArrayList<>();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            AccountRepository accountRepository = new AccountRepository(db);
            personalDataRepository = new PersonalDataRepository(db);

            adapter = new TransactionAdapter(getContext(), transactionList, accountRepository);
            binding.recyclerView.setAdapter(adapter);
            binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            emptyStateImageView = binding.emptyStateImageView;
            emptyStateTextView = binding.emptyStateTextView;

            calendar = Calendar.getInstance();
            setLocaleFromPreferences();
            updateDateText();
            TextView currentDateText = binding.currentDate;
            currentDateText.setOnClickListener(v -> showDatePicker());

            selectDayButton();
            setupButtonListeners();

            Button addTransactionButton = binding.addTransactionButton;
            addTransactionButton.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                navController.navigate(R.id.addTransactionFragment);
            });

            loadUserData();

            return root;
        }

        private void setLocaleFromPreferences() {
            SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MODE", Context.MODE_PRIVATE);
            String selectedLanguage = sharedPreferences.getString("selectedLanguage", "en");
            setLocale(selectedLanguage);
        }

        private void setLocale(String languageCode) {
            Locale locale = new Locale(languageCode);
            Locale.setDefault(locale);
            Configuration config = getResources().getConfiguration();
            config.setLocale(locale);
            Context context = getActivity().createConfigurationContext(config);
            getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        }

        private void selectDayButton() {
            setSelectedButton(binding.dayBtn, binding.monthlyBtn);
            Constants.SELECTED_TAB = Constants.DAILY;
            resetToCurrentDate();
            updateDateText();
        }

        private void resetToCurrentDate() {
            calendar = Calendar.getInstance();
        }

        private void setupButtonListeners() {
            binding.dayBtn.setOnClickListener(v -> {
                setSelectedButton(binding.dayBtn, binding.monthlyBtn);
                Constants.SELECTED_TAB = Constants.DAILY;
                resetToCurrentDate();
                updateDateText();
                loadTransactions();
            });

            binding.monthlyBtn.setOnClickListener(v -> {
                setSelectedButton(binding.monthlyBtn, binding.dayBtn);
                Constants.SELECTED_TAB = Constants.MONTHLY;
                resetToCurrentDate();
                updateDateText();
                loadTransactions();
            });

            binding.previousDate.setOnClickListener(v -> {
                if (Constants.SELECTED_TAB == Constants.DAILY) {
                    calendar.add(Calendar.DATE, -1);
                } else {
                    calendar.add(Calendar.MONTH, -1);
                }
                updateDateText();
                loadTransactions();
            });

            binding.nextDate.setOnClickListener(v -> {
                if (Constants.SELECTED_TAB == Constants.DAILY) {
                    calendar.add(Calendar.DATE, 1);
                } else {
                    calendar.add(Calendar.MONTH, 1);
                }
                updateDateText();
                loadTransactions();
            });
        }

        private void setSelectedButton(TextView selected, TextView unselected) {
            boolean isDarkTheme = (getActivity().getResources().getConfiguration().uiMode &
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

            if (isDarkTheme) {
                selected.setBackgroundResource(R.drawable.day_month_selector_night);
                selected.setTextColor(getResources().getColor(android.R.color.white));
                unselected.setBackgroundResource(R.drawable.day_month_selector_white);
                unselected.setTextColor(getResources().getColor(android.R.color.white));
            } else {
                selected.setBackgroundResource(R.drawable.day_month_selector_active);
                selected.setTextColor(getResources().getColor(android.R.color.white));
                unselected.setBackgroundResource(R.drawable.day_month_selector);
                unselected.setTextColor(getResources().getColor(android.R.color.black));
            }
        }

        private void updateDateText() {
            TextView currentDateText = binding.currentDate;
            if (Constants.SELECTED_TAB == Constants.DAILY) {
                currentDateText.setText(formatDate(calendar.getTime()));
            } else {
                currentDateText.setText(formatDateByMonth(calendar.getTime()));
            }
            binding.dateText.setText(formatDateShort(Calendar.getInstance().getTime()));
        }

        public static String formatDateShort(Date date) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM", Locale.getDefault());
            return dateFormat.format(date);
        }

        public static String formatDate(Date date) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM, yyyy", Locale.getDefault());
            return dateFormat.format(date);
        }

        public static String formatDateByMonth(Date date) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault());
            return dateFormat.format(date);
        }

        private void loadUserData() {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                return;
            }

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            personalDataRepository.getPersonalDataById(userId).thenAccept(personalData -> {
                if (personalData != null) {
                    this.personalData = personalData;
                    loadTransactions();
                    loadAllTransactions();
                } else {
                }
            }).exceptionally(e -> {
                return null;
            });
        }

        private void loadTransactions() {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                return;
            }

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            Calendar startCalendar = (Calendar) calendar.clone();
            Calendar endCalendar = (Calendar) calendar.clone();

            if (Constants.SELECTED_TAB == Constants.DAILY) {
                startCalendar.set(Calendar.HOUR_OF_DAY, 0);
                startCalendar.set(Calendar.MINUTE, 0);
                startCalendar.set(Calendar.SECOND, 0);
                startCalendar.set(Calendar.MILLISECOND, 0);

                endCalendar.set(Calendar.HOUR_OF_DAY, 23);
                endCalendar.set(Calendar.MINUTE, 59);
                endCalendar.set(Calendar.SECOND, 59);
                endCalendar.set(Calendar.MILLISECOND, 999);
            } else {
                startCalendar.set(Calendar.DAY_OF_MONTH, 1);
                startCalendar.set(Calendar.HOUR_OF_DAY, 0);
                startCalendar.set(Calendar.MINUTE, 0);
                startCalendar.set(Calendar.SECOND, 0);
                startCalendar.set(Calendar.MILLISECOND, 0);

                endCalendar.set(Calendar.DAY_OF_MONTH, startCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                endCalendar.set(Calendar.HOUR_OF_DAY, 23);
                endCalendar.set(Calendar.MINUTE, 59);
                endCalendar.set(Calendar.SECOND, 59);
                endCalendar.set(Calendar.MILLISECOND, 999);
            }

            db.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .whereGreaterThanOrEqualTo("date", startCalendar.getTime())
                    .whereLessThanOrEqualTo("date", endCalendar.getTime())
                    .orderBy("date", Query.Direction.ASCENDING) // Сортируем по дате
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            return;
                        }

                        List<Transaction> newTransactions = new ArrayList<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot document : value) {
                                Transaction transaction = document.toObject(Transaction.class);
                                newTransactions.add(transaction);
                            }
                        }

                        if (adapter != null) {
                            adapter.updateTransactions(newTransactions);
                        }
                        handleEmptyState(newTransactions);
                    });
        }
        private void handleEmptyState(List<Transaction> transactions) {
            if (transactions.isEmpty()) {
                emptyStateImageView.setVisibility(View.VISIBLE);
                emptyStateTextView.setVisibility(View.VISIBLE);
                boolean isDarkTheme = (getActivity() != null && (getActivity().getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES);

                int imageResource = isDarkTheme ? R.drawable.document_search_night : R.drawable.document_search;

                if (getContext() != null) {
                    Glide.with(getContext())
                            .asGif()
                            .load(imageResource)
                            .into(emptyStateImageView);
                }
            } else {
                emptyStateImageView.setVisibility(View.GONE);
                emptyStateTextView.setVisibility(View.GONE);
            }
        }

        private void loadAllTransactions() {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                return;
            }

            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            transactionsListener = db.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            return;
                        }

                        List<Transaction> allTransactions = new ArrayList<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot document : value) {
                                Transaction transaction = document.toObject(Transaction.class);
                                allTransactions.add(transaction);
                            }
                        }
                        updateAmounts(allTransactions);
                    });
        }
        private void showDatePicker() {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            new android.app.DatePickerDialog(getActivity(), (view, selectedYear, selectedMonth, selectedDay) -> {
                calendar.set(selectedYear, selectedMonth, selectedDay);
                updateDateText();
                loadTransactions();
            }, year, month, day).show();
        }

        private void updateAmounts(List<Transaction> transactions) {
            if (binding == null) {
                return;
            }

            double totalBalance = 0;
            double totalIncome = 0;
            double totalExpense = 0;

            for (Transaction transaction : transactions) {
                double amount = transaction.getAmount();
                if (personalData != null && !transaction.currency.equals(personalData.currency)) {
                    amount = convertCurrency(amount, transaction.currency, personalData.currency);
                }

                totalBalance += amount;
                if (transaction.getType().equals("DOHOD")) {
                    totalIncome += amount;
                } else if (transaction.getType().equals("RACHOD")) {
                    totalExpense += amount;
                }
            }

            String currency = personalData != null ? personalData.currency : "₩";
            binding.balanceAmount.setText(String.format("%s %.2f", currency, totalBalance));
            binding.incomeAmount.setText(String.format("%s %.2f", currency, totalIncome));
            binding.expenseAmount.setText(String.format("%s %.2f", currency, totalExpense));
        }


        private double convertCurrency(double amount, String fromCurrency, String toCurrency) {
            if (fromCurrency == null || toCurrency == null || fromCurrency.equals(toCurrency)) {
                return amount;
            }

            double rate = 1.0;

            if (fromCurrency.equals("USD")) {
                if (toCurrency.equals("EUR")) {
                    rate = 0.85;
                } else if (toCurrency.equals("RUB")) {
                    rate = 70.0;
                } else if (toCurrency.equals("BYN")) {
                    rate = 2.6;
                } else if (toCurrency.equals("UAH")) {
                    rate = 27.0;
                } else if (toCurrency.equals("PLN")) {
                    rate = 3.7;
                }
            } else if (fromCurrency.equals("EUR")) {
                if (toCurrency.equals("USD")) {
                    rate = 1.18;
                } else if (toCurrency.equals("RUB")) {
                    rate = 82.0;
                } else if (toCurrency.equals("BYN")) {
                    rate = 3.1;
                } else if (toCurrency.equals("UAH")) {
                    rate = 31.0;
                } else if (toCurrency.equals("PLN")) {
                    rate = 4.3;
                }
            } else if (fromCurrency.equals("RUB")) {
                if (toCurrency.equals("USD")) {
                    rate = 0.014;
                } else if (toCurrency.equals("EUR")) {
                    rate = 0.012;
                } else if (toCurrency.equals("BYN")) {
                    rate = 0.032;
                } else if (toCurrency.equals("UAH")) {
                    rate = 0.36;
                } else if (toCurrency.equals("PLN")) {
                    rate = 0.05;
                }
            } else if (fromCurrency.equals("BYN")) {
                if (toCurrency.equals("USD")) {
                    rate = 0.38;
                } else if (toCurrency.equals("EUR")) {
                    rate = 0.32;
                } else if (toCurrency.equals("RUB")) {
                    rate = 31.0;
                } else if (toCurrency.equals("UAH")) {
                    rate = 11.0;
                } else if (toCurrency.equals("PLN")) {
                    rate = 1.4;
                }
            } else if (fromCurrency.equals("UAH")) {
                if (toCurrency.equals("USD")) {
                    rate = 0.037;
                } else if (toCurrency.equals("EUR")) {
                    rate = 0.032;
                } else if (toCurrency.equals("RUB")) {
                    rate = 2.8;
                } else if (toCurrency.equals("BYN")) {
                    rate = 0.091;
                } else if (toCurrency.equals("PLN")) {
                    rate = 0.12;
                }
            } else if (fromCurrency.equals("PLN")) {
                if (toCurrency.equals("USD")) {
                    rate = 0.27;
                } else if (toCurrency.equals("EUR")) {
                    rate = 0.23;
                } else if (toCurrency.equals("RUB")) {
                    rate = 20.0;
                } else if (toCurrency.equals("BYN")) {
                    rate = 0.71;
                } else if (toCurrency.equals("UAH")) {
                    rate = 8.4;
                }
            }
            return amount * rate;
        }

        @Override
        public void onResume() {
            super.onResume();
            setLocaleFromPreferences();
            updateDateText();
        }
        @Override
        public void onDestroyView() {
            super.onDestroyView();
            if (transactionsListener != null) {
                transactionsListener.remove();
            }
            binding = null;
        }
    }