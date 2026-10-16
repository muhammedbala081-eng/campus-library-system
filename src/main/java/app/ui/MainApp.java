package app.ui;


import app.dao.*;
import app.model.*;
import app.service.LendingService;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainApp extends Application {

    private MemberDao memberDao;
    private BookDao bookDao;
    private LoanDao loanDao;
    private LendingService lendingService;

    // Observable table datasets
    private final ObservableList<Loan> activeLoansData = FXCollections.observableArrayList();
    private final ObservableList<Book> booksData = FXCollections.observableArrayList();
    private final ObservableList<Member> membersData = FXCollections.observableArrayList();

    private TableView<Loan> loansTable;
    private TableView<Book> booksTable;
    private TableView<Member> membersTable;

    @Override
    public void start(Stage stage) {
        try {
            DatabaseManager.initializeDatabase();
            memberDao = new SqliteMemberDao();
            bookDao = new SqliteBookDao();
            loanDao = new SqliteLoanDao();
            lendingService = new LendingService(memberDao, bookDao, loanDao);
        } catch (Exception e) {
            showError("Startup Error", "Could not connect to database: " + e.getMessage());
            return;
        }

        // Header
        VBox header = createHeader();

        // Tabs
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab lendingTab = new Tab("Lending Desk", createLendingPane());
        Tab catalogTab = new Tab("Book Catalog", createCatalogPane());
        Tab membersTab = new Tab("Member Management", createMembersPane());

        tabPane.getTabs().addAll(lendingTab, catalogTab, membersTab);

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(tabPane);

        Scene scene = new Scene(root, 1024, 680);
        stage.setTitle("Campus Library Management System - CSC 508");
        stage.setScene(scene);
        stage.show();

        // Initial Data Load
        refreshAllData();
    }

    private VBox createHeader() {
        Label title = new Label("Campus Library Management System");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label subtitle = new Label("CSC 508: Full-Stack Java (JavaFX, 3-Tier Architecture, SQLite JDBC)");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #93c5fd;");

        VBox header = new VBox(4, title, subtitle);
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setStyle("-fx-background-color: #1e3a8a;");
        return header;
    }

    // ==========================================
    // TAB 1: LENDING DESK (ISSUE & RETURN)
    // ==========================================
    private Pane createLendingPane() {
        VBox mainBox = new VBox(15);
        mainBox.setPadding(new Insets(15));

        // Form to Issue Book
        TitledPane issueBox = new TitledPane();
        issueBox.setText("Issue Book to Member");
        issueBox.setCollapsible(false);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField memberIdField = new TextField();
        memberIdField.setPromptText("e.g. CSC/2026/001");

        TextField barcodeField = new TextField();
        barcodeField.setPromptText("e.g. BC-CS-0001");

        Button issueBtn = new Button("Issue Book");
        issueBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold;");

        Label issueFeedback = new Label();

        issueBtn.setOnAction(e -> {
            String studentId = memberIdField.getText().trim();
            String barcode = barcodeField.getText().trim();
            if (studentId.isEmpty() || barcode.isEmpty()) {
                issueFeedback.setText("Please enter both Member ID and Copy Barcode.");
                issueFeedback.setStyle("-fx-text-fill: #dc2626;");
                return;
            }
            try {
                Loan loan = lendingService.issueBook(studentId, barcode);
                issueFeedback.setText("Success! Loan #" + loan.getLoanId() + " issued. Due: " + loan.getDueDate());
                issueFeedback.setStyle("-fx-text-fill: #059669;");
                memberIdField.clear();
                barcodeField.clear();
                refreshAllData();
            } catch (Exception ex) {
                issueFeedback.setText("Issue Failed: " + ex.getMessage());
                issueFeedback.setStyle("-fx-text-fill: #dc2626;");
            }
        });

        grid.add(new Label("Member ID:"), 0, 0);
        grid.add(memberIdField, 1, 0);
        grid.add(new Label("Copy Barcode:"), 2, 0);
        grid.add(barcodeField, 3, 0);
        grid.add(issueBtn, 4, 0);
        grid.add(issueFeedback, 1, 1, 4, 1);
        issueBox.setContent(grid);

        // Active Loans Table
        loansTable = new TableView<>();
        loansTable.setItems(activeLoansData);

        TableColumn<Loan, String> colLoanId = new TableColumn<>("Loan ID");
        colLoanId.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getLoanId())));
        colLoanId.setPrefWidth(70);

        TableColumn<Loan, String> colMember = new TableColumn<>("Member");
        colMember.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMemberName()));
        colMember.setPrefWidth(180);

        TableColumn<Loan, String> colBook = new TableColumn<>("Book Title");
        colBook.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookTitle()));
        colBook.setPrefWidth(240);

        TableColumn<Loan, String> colBarcode = new TableColumn<>("Barcode");
        colBarcode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCopyBarcode()));
        colBarcode.setPrefWidth(120);

        TableColumn<Loan, String> colDue = new TableColumn<>("Due Date");
        colDue.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDueDate() != null ? d.getValue().getDueDate().toString() : ""));
        colDue.setPrefWidth(110);

        TableColumn<Loan, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        colStatus.setPrefWidth(90);

        loansTable.getColumns().addAll(colLoanId, colMember, colBook, colBarcode, colDue, colStatus);

        Button returnBtn = new Button("Process Return for Selected Book");
        returnBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold;");
        returnBtn.setOnAction(e -> {
            Loan selected = loansTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showInfo("Select Loan", "Please select a loan from the table to return.");
                return;
            }
            try {
                lendingService.returnBook(selected.getLoanId());
                showInfo("Book Returned", "Book copy successfully returned to catalog inventory.");
                refreshAllData();
            } catch (Exception ex) {
                showError("Return Error", ex.getMessage());
            }
        });

        HBox tableActions = new HBox(10, returnBtn);
        tableActions.setAlignment(Pos.CENTER_LEFT);

        VBox activeLoansSection = new VBox(8, new Label("Active Borrowed Books:"), loansTable, tableActions);
        VBox.setVgrow(loansTable, Priority.ALWAYS);

        mainBox.getChildren().addAll(issueBox, activeLoansSection);
        VBox.setVgrow(activeLoansSection, Priority.ALWAYS);
        return mainBox;
    }

    // ==========================================
    // TAB 2: BOOK CATALOG
    // ==========================================
    private Pane createCatalogPane() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(15));

        // Search Bar
        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        TextField searchField = new TextField();
        searchField.setPromptText("Search by Title, Author, or ISBN...");
        searchField.setPrefWidth(300);

        Button searchBtn = new Button("Search");
        Button resetBtn = new Button("Reset");

        searchBtn.setOnAction(e -> {
            String query = searchField.getText().trim();
            if (query.isEmpty()) {
                refreshBooks();
            } else {
                try {
                    booksData.setAll(bookDao.search(query));
                } catch (SQLException ex) {
                    showError("Search Error", ex.getMessage());
                }
            }
        });

        resetBtn.setOnAction(e -> {
            searchField.clear();
            refreshBooks();
        });

        searchBar.getChildren().addAll(new Label("Catalog Search:"), searchField, searchBtn, resetBtn);

        // Books Table
        booksTable = new TableView<>();
        booksTable.setItems(booksData);

        TableColumn<Book, String> colIsbn = new TableColumn<>("ISBN");
        colIsbn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getIsbn()));
        colIsbn.setPrefWidth(130);

        TableColumn<Book, String> colTitle = new TableColumn<>("Title");
        colTitle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));
        colTitle.setPrefWidth(250);

        TableColumn<Book, String> colAuthor = new TableColumn<>("Author");
        colAuthor.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAuthor()));
        colAuthor.setPrefWidth(160);

        TableColumn<Book, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoryName()));
        colCat.setPrefWidth(160);

        TableColumn<Book, String> colAvailable = new TableColumn<>("Available / Total");
        colAvailable.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAvailableCopies() + " / " + d.getValue().getTotalCopies()));
        colAvailable.setPrefWidth(120);

        booksTable.getColumns().addAll(colIsbn, colTitle, colAuthor, colCat, colAvailable);
        VBox.setVgrow(booksTable, Priority.ALWAYS);

        box.getChildren().addAll(searchBar, booksTable);
        return box;
    }

    // ==========================================
    // TAB 3: MEMBERS MANAGEMENT
    // ==========================================
    private Pane createMembersPane() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(15));

        // Add Member Form
        TitledPane registerPane = new TitledPane();
        registerPane.setText("Register New Member");
        registerPane.setCollapsible(false);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));

        TextField idField = new TextField();
        idField.setPromptText("e.g. STU/2026/045");

        TextField nameField = new TextField();
        nameField.setPromptText("Full Name");

        TextField emailField = new TextField();
        emailField.setPromptText("user@campus.edu.ng");

        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("STUDENT", "FACULTY", "STAFF"));
        typeCombo.setValue("STUDENT");

        Button addBtn = new Button("Register Member");
        addBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold;");

        addBtn.setOnAction(e -> {
            String id = idField.getText().trim();
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String type = typeCombo.getValue();

            if (id.isEmpty() || name.isEmpty() || email.isEmpty()) {
                showError("Validation Error", "Please fill in ID, Full Name, and Email.");
                return;
            }

            int maxBooks = "FACULTY".equals(type) ? 5 : ("STAFF".equals(type) ? 4 : 3);
            Member m = new Member(0, id, name, email, "", type, maxBooks, "ACTIVE", null);
            try {
                memberDao.save(m);
                showInfo("Member Registered", name + " registered successfully.");
                idField.clear();
                nameField.clear();
                emailField.clear();
                refreshMembers();
            } catch (Exception ex) {
                showError("Registration Error", ex.getMessage());
            }
        });

        grid.add(new Label("ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Name:"), 2, 0);
        grid.add(nameField, 3, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Role:"), 2, 1);
        grid.add(typeCombo, 3, 1);
        grid.add(addBtn, 4, 1);

        registerPane.setContent(grid);

        // Members Table
        membersTable = new TableView<>();
        membersTable.setItems(membersData);

        TableColumn<Member, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStudentStaffId()));
        colId.setPrefWidth(120);

        TableColumn<Member, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        colName.setPrefWidth(200);

        TableColumn<Member, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));
        colEmail.setPrefWidth(200);

        TableColumn<Member, String> colRole = new TableColumn<>("Type");
        colRole.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMembershipType()));
        colRole.setPrefWidth(100);

        TableColumn<Member, String> colLimit = new TableColumn<>("Max Books");
        colLimit.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getMaxBooksAllowed())));
        colLimit.setPrefWidth(90);

        TableColumn<Member, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        colStatus.setPrefWidth(90);

        membersTable.getColumns().addAll(colId, colName, colEmail, colRole, colLimit, colStatus);
        VBox.setVgrow(membersTable, Priority.ALWAYS);

        box.getChildren().addAll(registerPane, membersTable);
        return box;
    }

    // ==========================================
    // REFRESH HELPERS
    // ==========================================
    private void refreshAllData() {
        refreshLoans();
        refreshBooks();
        refreshMembers();
    }

    private void refreshLoans() {
        try {
            activeLoansData.setAll(lendingService.getActiveLoans());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshBooks() {
        try {
            booksData.setAll(bookDao.findAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void refreshMembers() {
        try {
            membersData.setAll(memberDao.findAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
