    package com.mgke.da.ui.accounts;

    import android.os.Bundle;
    import android.view.LayoutInflater;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.ArrayAdapter;
    import android.widget.Button;
    import android.widget.EditText;
    import com.google.firebase.auth.FirebaseAuth;
    import android.text.InputFilter;
    import android.text.Spanned;
    import com.google.firebase.auth.FirebaseUser;
    import android.widget.GridLayout;
    import android.widget.ImageView;
    import android.widget.Spinner;
    import android.widget.Toast;
    import androidx.fragment.app.Fragment;
    import androidx.navigation.NavController;
    import androidx.navigation.Navigation;
    import com.google.firebase.firestore.FirebaseFirestore;
    import com.mgke.da.R;
    import com.mgke.da.models.Account;
    import com.mgke.da.repository.AccountRepository;

    public class AddAccountFragment extends Fragment {

        private String selectedBackground = null;
        private GridLayout iconSelectionGrid;

        public AddAccountFragment() {
            // Required empty public constructor
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_add_account, container, false);

            ImageView backButton = view.findViewById(R.id.backButton);
            EditText accountNameEditText = view.findViewById(R.id.accountNameEditText);
            Button saveAccountButton = view.findViewById(R.id.saveAccountButton);
            Spinner currencySpinner = view.findViewById(R.id.currencySpinner);
            iconSelectionGrid = view.findViewById(R.id.iconSelectionGrid);

            EditText accountAmountEditText = view.findViewById(R.id.accountAmountEditText);
            // Установка фильтра для ввода
            accountAmountEditText.setFilters(new InputFilter[]{new DecimalInputFilter()});

            String[] currencies = {"BYN", "USD", "EUR", "RUB", "GBP", "CHF"};
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, currencies);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            currencySpinner.setAdapter(adapter);

            // Настройка кликов для иконок
            for (int i = 0; i < iconSelectionGrid.getChildCount(); i++) {
                ImageView icon = (ImageView) iconSelectionGrid.getChildAt(i);
                icon.setOnClickListener(v -> {
                    highlightSelectedIcon(icon);
                });
            }

            // Установите обработчик клика для backButton
            backButton.setOnClickListener(v -> {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                navController.navigate(R.id.navigation_accounts);
            });

            saveAccountButton.setOnClickListener(v -> {
                String accountName = accountNameEditText.getText().toString().trim();
                String selectedCurrency = currencySpinner.getSelectedItem() != null ? currencySpinner.getSelectedItem().toString() : null;
                String accountAmountString = accountAmountEditText.getText().toString().trim(); // Считываем сумму

                if (accountName.isEmpty()) {
                    Toast.makeText(getActivity(), "Название счёта не должно быть пустым", Toast.LENGTH_SHORT).show();
                } else if (selectedCurrency == null || selectedCurrency.isEmpty()) {
                    Toast.makeText(getActivity(), "Выберите валюту", Toast.LENGTH_SHORT).show();
                } else if (accountAmountString.isEmpty()) { // Проверка на пустую сумму
                    Toast.makeText(getActivity(), "Введите сумму счёта", Toast.LENGTH_SHORT).show();
                } else if (selectedBackground == null) {
                    Toast.makeText(getActivity(), "Выберите фон для счёта", Toast.LENGTH_SHORT).show();
                } else {
                    // Создание нового счёта
                    Account newAccount = new Account();
                    newAccount.accountName = accountName;
                    newAccount.accountAmount = parseAccountAmount(accountAmountString); // Преобразование суммы
                    newAccount.userId = getCurrentUserId(); // Получите ID текущего пользователя
                    newAccount.currency = selectedCurrency;
                    newAccount.background = selectedBackground; // Сохраняем имя файла в объекте Account

                    // Сохранение счёта в базе данных
                    AccountRepository accountRepository = new AccountRepository(FirebaseFirestore.getInstance());
                    accountRepository.addAccount(newAccount);

                    // Показать сообщение об успешном добавлении
                    Toast.makeText(getActivity(), "Счёт добавлен", Toast.LENGTH_SHORT).show();
                    // Вернуться на экран со счётами
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                    navController.navigate(R.id.navigation_accounts);
                }
            });

            return view;
        }

        // Метод для преобразования суммы
        private double parseAccountAmount(String amountString) {
            // Заменяем запятую на точку для правильного парсинга
            amountString = amountString.replace(',', '.');
            return Double.parseDouble(amountString); // Преобразуем строку в число
        }

        private void highlightSelectedIcon(ImageView selectedIcon) {
            // Сброс фона и масштаба для всех иконок
            if (iconSelectionGrid != null) {
                for (int i = 0; i < iconSelectionGrid.getChildCount(); i++) {
                    ImageView icon = (ImageView) iconSelectionGrid.getChildAt(i);
                    // Устанавливаем оригинальные фоны для каждой иконки
                    if (icon.getId() == R.id.icon1) {
                        icon.setBackgroundResource(R.drawable.account_fon1);
                    } else if (icon.getId() == R.id.icon2) {
                        icon.setBackgroundResource(R.drawable.account_fon2);
                    } else if (icon.getId() == R.id.icon3) {
                        icon.setBackgroundResource(R.drawable.account_fon3);
                    } else if (icon.getId() == R.id.icon4) {
                        icon.setBackgroundResource(R.drawable.account_fon4);
                    } else if (icon.getId() == R.id.icon5) {
                        icon.setBackgroundResource(R.drawable.account_fon5);
                    }
                    // Сбрасываем масштаб для всех иконк
                    icon.setScaleX(1.0f);
                    icon.setScaleY(1.0f);
                }
            }

            // Увеличиваем масштаб для выделенной иконки
            selectedIcon.setScaleX(1.2f);
            selectedIcon.setScaleY(1.2f);

            // Сохраняем имя файла фона
            if (selectedIcon.getId() == R.id.icon1) {
                selectedBackground = "account_fon1";
            } else if (selectedIcon.getId() == R.id.icon2) {
                selectedBackground = "account_fon2";
            } else if (selectedIcon.getId() == R.id.icon3) {
                selectedBackground = "account_fon3";
            } else if (selectedIcon.getId() == R.id.icon4) {
                selectedBackground = "account_fon4";
            } else if (selectedIcon.getId() == R.id.icon5) {
                selectedBackground = "account_fon5";
            }
        }

        private String getCurrentUserId() {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            return user != null ? user.getUid() : null; // Возвращает ID пользователя или null, если не аутентифицирован
        }

        // Фильтр для ввода только цифр, точки и запятой
        public class DecimalInputFilter implements InputFilter {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    char character = source.charAt(i);
                    // Проверяем, является ли символ цифрой, точкой или запятой
                    if (!Character.isDigit(character) && character != '.' && character != ',') {
                        return ""; // Запретить ввод других символов
                    }
                }
                return null; // Разрешить ввод
            }
        }
    }