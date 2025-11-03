# 🎨 Unified Layout System - Implementation Guide

## Overview
This guide explains how to implement a consistent, user-friendly layout across all POS screens with a persistent sidebar navigation.

## 🏗️ Layout Structure

All main application screens (except Login/Signup) should follow this structure:

```
┌─────────────────────────────────────────────────┐
│  Sidebar      │    Main Content Area           │
│  (260px)      │                                │
│               │                                │
│  Navigation   │    Page Title                  │
│               │    ───────────────             │
│  - Dashboard  │                                │
│  - Customers  │    Content Cards               │
│  - Products   │                                │
│  - Orders     │                                │
│  - Returns    │                                │
│  - Reports    │                                │
│               │                                │
│  User Info    │                                │
│  [Logout]     │                                │
└─────────────────────────────────────────────────┘
```

## 📋 Implementation Steps

### Step 1: Update FXML Structure

Replace the existing layout with this unified template:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import com.jfoenix.controls.JFXButton?>
<?import javafx.geometry.Insets?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>
<?import javafx.scene.text.Text?>
<?import javafx.scene.text.Font?>

<AnchorPane fx:id="context" styleClass="background-modern" 
            xmlns="http://javafx.com/javafx/21" 
            xmlns:fx="http://javafx.com/fxml/1"
            fx:controller="com.devstack.pos.controller.YourController"
            stylesheets="@styles/pos-styles.css">
    
    <!-- SIDEBAR (Fixed 260px width) -->
    <VBox prefWidth="260" spacing="12" styleClass="sidebar-modern"
          AnchorPane.leftAnchor="0" 
          AnchorPane.topAnchor="0" 
          AnchorPane.bottomAnchor="0">
        
        <padding><Insets top="20" right="20" bottom="20" left="20"/></padding>
        
        <!-- Logo/Brand -->
        <VBox spacing="8" style="-fx-padding: 0 0 20 0;">
            <Text text="🏪 Lilarathne POS" styleClass="sidebar-brand"/>
            <Text text="Point of Sale System" styleClass="sidebar-subtitle"/>
        </VBox>
        
        <Separator style="-fx-background-color: #334155;"/>
        
        <!-- Navigation Menu -->
        <VBox spacing="8" VBox.vgrow="ALWAYS" style="-fx-padding: 10 0 0 0;">
            <JFXButton text="🏠  Dashboard" onAction="#btnDashboardOnAction" styleClass="sidebar-btn"/>
            <JFXButton text="👥  Customers" onAction="#btnCustomerOnAction" styleClass="sidebar-btn"/>
            <JFXButton text="📦  Products" onAction="#btnProductOnAction" styleClass="sidebar-btn"/>
            <JFXButton text="🛒  Place Order" onAction="#btnPlaceOrderOnAction" styleClass="sidebar-btn"/>
            <JFXButton text="📋  Order Details" onAction="#btnOrderDetailsOnAction" styleClass="sidebar-btn"/>
            <JFXButton text="🔄  Returns" onAction="#btnReturnsOnAction" styleClass="sidebar-btn"/>
            <JFXButton text="💰  Purchasing" onAction="#btnPurchaseOnAction" styleClass="sidebar-btn"/>
            <JFXButton text="📊  Reports" onAction="#btnReportsOnAction" styleClass="sidebar-btn"/>
        </VBox>
        
        <Separator style="-fx-background-color: #334155;"/>
        
        <!-- User Info & Logout -->
        <VBox spacing="12" style="-fx-padding: 10 0 0 0;">
            <VBox spacing="4" styleClass="user-card">
                <Text fx:id="txtUserEmail" text="Loading..." styleClass="user-email"/>
                <Text fx:id="txtUserRole" text="Loading..." styleClass="user-role"/>
            </VBox>
            <JFXButton text="🚪  Logout" onAction="#btnLogoutOnAction" styleClass="sidebar-btn-logout"/>
        </VBox>
    </VBox>
    
    <!-- MAIN CONTENT AREA -->
    <VBox spacing="24" styleClass="main-content"
          AnchorPane.leftAnchor="260" 
          AnchorPane.topAnchor="0" 
          AnchorPane.rightAnchor="0" 
          AnchorPane.bottomAnchor="0">
        
        <!-- Page Header -->
        <HBox alignment="CENTER_LEFT" spacing="16" styleClass="content-header">
            <VBox spacing="4" HBox.hgrow="ALWAYS">
                <Text text="Page Title" styleClass="page-title"/>
                <Text text="Page description or breadcrumb" styleClass="page-subtitle"/>
            </VBox>
            <Region HBox.hgrow="SOMETIMES"/>
            <!-- Add action buttons here if needed -->
        </HBox>
        
        <!-- YOUR PAGE CONTENT HERE -->
        <VBox spacing="20" VBox.vgrow="ALWAYS">
            <!-- Cards, forms, tables, etc. -->
        </VBox>
        
    </VBox>
</AnchorPane>
```

### Step 2: Update Controller - Add Common Navigation Methods

Add these navigation methods to EVERY controller:

```java
@FXML
public void initialize() {
    // Initialize user info in sidebar
    if (txtUserEmail != null) {
        txtUserEmail.setText(UserSessionData.email);
    }
    if (txtUserRole != null) {
        txtUserRole.setText("ROLE: " + UserSessionData.userRole);
    }
    
    // Your existing initialization code...
}

@FXML
public void btnDashboardOnAction(ActionEvent event) throws IOException {
    if (!AuthorizationUtil.checkAndAlert(true)) return;
    setUi("DashboardForm");
}

@FXML
public void btnCustomerOnAction(ActionEvent event) throws IOException {
    if (!AuthorizationUtil.canAccessCustomers()) {
        AuthorizationUtil.showUnauthorizedAlert();
        return;
    }
    setUi("CustomerForm");
}

@FXML
public void btnProductOnAction(ActionEvent event) throws IOException {
    if (!AuthorizationUtil.canAccessProducts()) {
        AuthorizationUtil.showAdminOnlyAlert();
        return;
    }
    setUi("ProductMainForm");
}

@FXML
public void btnPlaceOrderOnAction(ActionEvent event) throws IOException {
    if (!AuthorizationUtil.canAccessPOSOrders()) {
        AuthorizationUtil.showUnauthorizedAlert();
        return;
    }
    setUi("PlaceOrderForm");
}

@FXML
public void btnOrderDetailsOnAction(ActionEvent event) throws IOException {
    setUi("OrderDetailsForm");
}

@FXML
public void btnReturnsOnAction(ActionEvent event) throws IOException {
    if (!AuthorizationUtil.canAccessReturnOrders()) {
        AuthorizationUtil.showUnauthorizedAlert();
        return;
    }
    setUi("ReturnOrdersForm");
}

@FXML
public void btnPurchaseOnAction(ActionEvent event) throws IOException {
    if (!AuthorizationUtil.canAccessPurchaseOrders()) {
        AuthorizationUtil.showAdminOnlyAlert();
        return;
    }
    setUi("PurchaseOrdersForm");
}

@FXML
public void btnReportsOnAction(ActionEvent event) throws IOException {
    if (!AuthorizationUtil.canAccessReports()) {
        AuthorizationUtil.showAdminOnlyAlert();
        return;
    }
    setUi("AnalysisPage");
}

@FXML
public void btnLogoutOnAction(ActionEvent event) throws IOException {
    UserSessionData.clear();
    
    Stage stage = (Stage) context.getScene().getWindow();
    FXMLLoader loader = new FXMLLoader();
    loader.setLocation(getClass().getResource("/com/devstack/pos/view/LoginForm.fxml"));
    loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
    
    Parent root = loader.load();
    StageManager.loadAuthScene(stage, root);
}

private void setUi(String url) throws IOException {
    Stage stage = (Stage) context.getScene().getWindow();
    FXMLLoader loader = new FXMLLoader();
    loader.setLocation(getClass().getResource("/com/devstack/pos/view/" + url + ".fxml"));
    loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);
    
    Parent root = loader.load();
    StageManager.loadFullScreenScene(stage, root);
}
```

### Step 3: Add FXML Fields in Controller

```java
public class YourController {
    public AnchorPane context;
    
    @FXML
    private Text txtUserEmail;
    
    @FXML
    private Text txtUserRole;
    
    // Your other fields...
}
```

## 🎨 Design Guidelines

### Spacing & Padding
- **Sidebar padding**: 20px all sides
- **Main content padding**: 32px
- **Card spacing**: 20-24px between cards
- **Section spacing**: 24px between major sections

### Colors
- **Background**: `#f8fafc` (light gray-blue)
- **Sidebar**: `#1e293b` (dark slate)
- **Cards**: `#ffffff` (white)
- **Primary**: `#3b82f6` (blue)
- **Success**: `#10b981` (green)
- **Danger**: `#dc2626` (red)

### Typography
- **Page Title**: 28px, bold
- **Section Title**: 20px, bold
- **Body Text**: 14px
- **Small Text**: 12px

## 📱 Responsive Behavior

The layout automatically adjusts:
- **Sidebar**: Fixed 260px width
- **Content**: Flexible, takes remaining space
- **Full Screen**: Maximized by default
- **Cards**: Use GridPane with percentage columns for responsiveness

## ✅ Checklist for Each Screen

- [ ] Updated FXML with sidebar layout
- [ ] Added all navigation methods in controller
- [ ] Added user info fields (txtUserEmail, txtUserRole)
- [ ] Applied `styleClass="sidebar-btn-active"` to current page button
- [ ] Tested authorization for restricted pages
- [ ] Verified full-screen display
- [ ] Checked visual consistency with other screens

## 🔧 CSS Classes Reference

### Sidebar
- `sidebar-modern` - Main sidebar container
- `sidebar-brand` - Brand/logo text
- `sidebar-subtitle` - Subtitle under brand
- `sidebar-btn` - Navigation button
- `sidebar-btn-active` - Active/current page button
- `sidebar-btn-logout` - Logout button (red)
- `user-card` - User info card
- `user-email` - User email text
- `user-role` - User role text

### Content Area
- `main-content` - Main content container
- `content-header` - Page header card
- `page-title` - Page title text
- `page-subtitle` - Page subtitle/breadcrumb text
- `card` - Standard content card

## 📄 Example: Complete Customer Form

See the example implementation in the codebase for a fully working reference.

## 🚀 Migration Priority

1. **High Priority** (Main workflows)
   - Dashboard ✅
   - Customer Form
   - Place Order Form
   - Product Management
   
2. **Medium Priority**
   - Return Orders
   - Purchase Orders
   - Order Details

3. **Low Priority** (Reports & Analytics)
   - Analysis Page
   - Reports

## 💡 Tips

1. **Copy-Paste Template**: Use the FXML template above as a starting point
2. **Navigation Methods**: Copy all navigation methods - they're identical across controllers
3. **Active Button**: Change `styleClass="sidebar-btn"` to `sidebar-btn-active` for current page
4. **Test Authorization**: Verify role-based access works after migration
5. **Use StageManager**: Always use `StageManager.loadFullScreenScene()` for navigation

## 🐛 Troubleshooting

**Sidebar not showing?**
- Check AnchorPane anchors (leftAnchor, topAnchor, bottomAnchor)
- Verify CSS is loaded: `stylesheets="@styles/pos-styles.css"`

**Navigation not working?**
- Ensure all `onAction` methods exist in controller
- Check method names match FXML
- Verify `fx:controller` attribute is correct

**Layout broken in full screen?**
- Use VBox.vgrow="ALWAYS" for expanding content
- Use HBox.hgrow="ALWAYS" for flexible regions
- Check AnchorPane anchors are set correctly

---

**Need Help?** Review existing migrated screens as reference implementations.

