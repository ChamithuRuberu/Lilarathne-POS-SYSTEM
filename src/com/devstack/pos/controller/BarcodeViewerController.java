package com.devstack.pos.controller;

import com.devstack.pos.util.BarcodeGenerator;
import com.google.zxing.WriterException;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

@Component
public class BarcodeViewerController {
    
    @FXML
    public TextField txtProductCode;
    @FXML
    public TextArea txtDescription;
    @FXML
    public TextField txtBarcode;
    @FXML
    public ImageView imgBarcode;
    @FXML
    public Button btnDownload;
    @FXML
    public Button btnClose;
    
    private Stage stage;
    private String barcodeValue;
    private Image currentBarcodeImage;
    
    public void setData(int productCode, String description, String barcode) {
        this.barcodeValue = barcode;
        
        if (txtProductCode != null) {
            txtProductCode.setText(String.valueOf(productCode));
        }
        
        if (txtDescription != null) {
            txtDescription.setText(description);
        }
        
        if (txtBarcode != null) {
            txtBarcode.setText(barcode);
        }
        
        // Try to decode as Base64 image first, otherwise generate from barcode string
        if (imgBarcode == null) {
            System.err.println("WARNING: imgBarcode ImageView is null - FXML injection may have failed");
            return;
        }
        
        if (barcode != null && !barcode.isEmpty()) {
            boolean imageLoaded = false;
            
            // Try to decode as Base64 image first
            try {
                byte[] imageData = Base64.getDecoder().decode(barcode);
                Image decodedImage = new Image(new ByteArrayInputStream(imageData));
                if (decodedImage != null && !decodedImage.isError()) {
                    imgBarcode.setImage(decodedImage);
                    imgBarcode.setVisible(true);
                    currentBarcodeImage = decodedImage;
                    imageLoaded = true;
                    System.out.println("Base64 barcode image loaded successfully");
                } else {
                    System.err.println("Decoded image is null or has errors");
                }
            } catch (IllegalArgumentException e) {
                // Not Base64, will generate from string below
                System.out.println("Not Base64 format, will generate barcode from string");
                imageLoaded = false;
            } catch (Exception e) {
                // Base64 decode succeeded but Image creation failed, will generate from string
                System.err.println("Base64 image decode failed, generating from barcode string: " + e.getMessage());
                e.printStackTrace();
                imageLoaded = false;
            }
            
            // If Base64 image loading failed, generate barcode image from string
            if (!imageLoaded) {
                try {
                    System.out.println("Generating barcode image from string: " + barcode);
                    Image barcodeImage = BarcodeGenerator.generateBarcodeImage(barcode);
                    if (barcodeImage != null && !barcodeImage.isError()) {
                        imgBarcode.setImage(barcodeImage);
                        imgBarcode.setVisible(true);
                        currentBarcodeImage = barcodeImage;
                        System.out.println("Barcode image generated successfully");
                    } else {
                        System.err.println("Generated barcode image is null or has errors");
                        new Alert(Alert.AlertType.ERROR, "Failed to generate barcode image!").show();
                    }
                } catch (WriterException e) {
                    System.err.println("Error generating barcode image: " + e.getMessage());
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Error generating barcode image: " + e.getMessage()).show();
                } catch (Exception e) {
                    System.err.println("Unexpected error generating barcode: " + e.getMessage());
                    e.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Unexpected error: " + e.getMessage()).show();
                }
            }
        } else {
            System.err.println("Barcode value is null or empty");
        }
    }
    
    /**
     * Set data with pre-loaded image (for batch barcodes)
     */
    public void setDataWithImage(int productCode, String description, String barcodeValue, Image barcodeImage) {
        this.barcodeValue = barcodeValue;
        this.currentBarcodeImage = barcodeImage;
        
        if (txtProductCode != null) {
            txtProductCode.setText(String.valueOf(productCode));
        }
        
        if (txtDescription != null) {
            txtDescription.setText(description);
        }
        
        if (txtBarcode != null) {
            txtBarcode.setText(barcodeValue);
        }
        
        if (imgBarcode == null) {
            System.err.println("WARNING: imgBarcode ImageView is null - FXML injection may have failed");
            return;
        }
        
        if (barcodeImage != null && !barcodeImage.isError()) {
            imgBarcode.setImage(barcodeImage);
            imgBarcode.setVisible(true);
            System.out.println("Pre-loaded barcode image set successfully");
        } else {
            System.err.println("Barcode image is null or has errors, attempting to generate from barcode value");
            // Fallback: generate barcode from string if image is invalid
            if (barcodeValue != null && !barcodeValue.isEmpty()) {
                try {
                    Image generatedImage = BarcodeGenerator.generateBarcodeImage(barcodeValue);
                    imgBarcode.setImage(generatedImage);
                    imgBarcode.setVisible(true);
                    currentBarcodeImage = generatedImage;
                    System.out.println("Barcode image generated from value successfully");
                } catch (WriterException e) {
                    System.err.println("Error generating barcode image: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    @FXML
    public void btnDownloadOnAction() {
        if (currentBarcodeImage == null) {
            if (barcodeValue == null || barcodeValue.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "No barcode to download!").show();
                return;
            }
            
            // Generate barcode image if not already loaded
            try {
                currentBarcodeImage = BarcodeGenerator.generateBarcodeImage(barcodeValue);
            } catch (WriterException e) {
                new Alert(Alert.AlertType.ERROR, "Error generating barcode: " + e.getMessage()).show();
                return;
            }
        }
        
        try {
            // Convert JavaFX Image to BufferedImage
            BufferedImage bufferedImage = null;
            
            // Try to convert the current image
            if (currentBarcodeImage != null) {
                bufferedImage = javafx.embed.swing.SwingFXUtils.fromFXImage(currentBarcodeImage, null);
            }
            
            // If conversion failed or image is null, try to regenerate from barcode value
            if (bufferedImage == null) {
                if (barcodeValue != null && !barcodeValue.isEmpty()) {
                    try {
                        // Regenerate barcode image from the barcode string
                        BufferedImage regeneratedImage = com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage(
                            new com.google.zxing.oned.Code128Writer().encode(
                                barcodeValue,
                                com.google.zxing.BarcodeFormat.CODE_128,
                                300,
                                80
                            )
                        );
                        bufferedImage = regeneratedImage;
                    } catch (Exception e) {
                        new Alert(Alert.AlertType.ERROR, "Could not convert or regenerate barcode image!").show();
                        return;
                    }
                } else {
                    new Alert(Alert.AlertType.ERROR, "Could not convert barcode image! No barcode value available.").show();
                    return;
                }
            }
            
            // Show file chooser
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Barcode Image");
            String fileName = barcodeValue != null ? "barcode_" + barcodeValue.replaceAll("[^a-zA-Z0-9]", "_") : "barcode";
            fileChooser.setInitialFileName(fileName + ".png");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Image", "*.png")
            );
            
            File file = fileChooser.showSaveDialog(stage);
            
            if (file != null) {
                // Save the image
                ImageIO.write(bufferedImage, "PNG", file);
                new Alert(Alert.AlertType.INFORMATION, 
                    "Barcode saved successfully!\nLocation: " + file.getAbsolutePath()).show();
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error saving barcode: " + e.getMessage()).show();
        }
    }
    
    @FXML
    public void btnCloseOnAction() {
        if (stage != null) {
            stage.close();
        }
    }
}

