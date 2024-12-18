import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class Bank {
    private final AtomicInteger availableMoney;

    public Bank(int initialMoney) {
        this.availableMoney = new AtomicInteger(initialMoney);
    }

    public boolean withdraw(int amount) {
        if (availableMoney.get() >= amount) {
            availableMoney.addAndGet(-amount);
            return true;
        }
        return false;
    }

    public void deposit(int amount) {
        availableMoney.addAndGet(amount);
    }

    public int getBalance() {
        return availableMoney.get();
    }
}

class Transaction {
    private final String customerName;
    private final boolean isWithdrawal;
    private final int amount;

    public Transaction(String customerName, boolean isWithdrawal, int amount) {
        this.customerName = customerName;
        this.isWithdrawal = isWithdrawal;
        this.amount = amount;
    }

    public String getCustomerName() {
        return customerName;
    }

    public boolean isWithdrawal() {
        return isWithdrawal;
    }

    public int getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return customerName + (isWithdrawal ? " withdrew " : " deposited ") + amount;
    }
}

class Teller implements Runnable {
    private final BlockingQueue<Transaction> transactionQueue;
    private final Bank bank;

    public Teller(BlockingQueue<Transaction> transactionQueue, Bank bank) {
        this.transactionQueue = transactionQueue;
        this.bank = bank;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Transaction transaction = transactionQueue.take(); // Берём транзакцию из очереди
                if (transaction.isWithdrawal()) {
                    if (bank.withdraw(transaction.getAmount())) {
                        System.out.println(Thread.currentThread().getName() + " processed: " + transaction);
                    } else {
                        System.out.println(Thread.currentThread().getName() + " failed: Not enough money for " + transaction);
                    }
                } else {
                    bank.deposit(transaction.getAmount());
                    System.out.println(Thread.currentThread().getName() + " processed: " + transaction);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println(Thread.currentThread().getName() + " stopped.");
        }
    }
}

class Customer implements Runnable {
    private final BlockingQueue<Transaction> transactionQueue;
    private final String name;
    private final boolean isWithdrawal;
    private final int amount;

    public Customer(BlockingQueue<Transaction> transactionQueue, String name, boolean isWithdrawal, int amount) {
        this.transactionQueue = transactionQueue;
        this.name = name;
        this.isWithdrawal = isWithdrawal;
        this.amount = amount;
    }

    @Override
    public void run() {
        try {
            Transaction transaction = new Transaction(name, isWithdrawal, amount);
            transactionQueue.put(transaction); // Добавляем транзакцию в очередь
            System.out.println(name + " queued: " + transaction);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

class HorsemanBank {
    public static void main(String[] args) throws InterruptedException {
        Bank bank = new Bank(1000); // Начальный баланс
        BlockingQueue<Transaction> transactionQueue = new LinkedBlockingQueue<>(); // Очередь транзакций
        int numberOfTellers = 2; // Количество кассиров

        // Создаем и запускаем кассиров
        ExecutorService tellerService = Executors.newFixedThreadPool(numberOfTellers);
        for (int i = 1; i <= numberOfTellers; i++) {
            tellerService.submit(new Teller(transactionQueue, bank));
        }

        // Создаем клиентов
        Thread customer1 = new Thread(new Customer(transactionQueue, "Customer 1", true, 200)); // Снятие
        Thread customer2 = new Thread(new Customer(transactionQueue, "Customer 2", false, 300)); // Внесение
        Thread customer3 = new Thread(new Customer(transactionQueue, "Customer 3", true, 500)); // Снятие
        Thread customer4 = new Thread(new Customer(transactionQueue, "Customer 4", true, 100)); // Снятие
        Thread customer5 = new Thread(new Customer(transactionQueue, "Customer 5", false, 150)); // Внесение

        // Запускаем клиентов
        customer1.start();
        customer2.start();
        customer3.start();
        customer4.start();
        customer5.start();

        // Ждем завершения работы клиентов
        customer1.join();
        customer2.join();
        customer3.join();
        customer4.join();
        customer5.join();

        // Ожидание обработки всех транзакций
        Thread.sleep(2000);

        // Останавливаем кассиров
        tellerService.shutdownNow();
        tellerService.awaitTermination(1, TimeUnit.SECONDS);

        // Выводим финальный баланс
        System.out.println("Final bank balance: " + bank.getBalance());
    }
}
