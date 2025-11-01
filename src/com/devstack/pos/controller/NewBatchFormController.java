package com.devstack.pos.controller;

import com.devstack.pos.entity.ProductDetail;
import com.devstack.pos.service.ProductDetailService;
import com.devstack.pos.util.BarcodeGenerator;
import com.devstack.pos.view.tm.ProductDetailTm;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class NewBatchFormController {

    public ImageView barcodeImage;
    public TextField txtQty;
    public TextField txtSellingPrice;
    public TextField txtShowPrice;
    public TextField txtBuyingPrice;
    public TextField txtProductCode;
    public TextArea txtSelectedProdDescription;
    public RadioButton rBtnYes;
    String uniqueData = null;
    BufferedImage bufferedImage = null;
    Stage stage = null;

    private final ProductDetailService productDetailService;

    public void initialize() throws WriterException {
    }

    private void setBarcode() throws WriterException {
        // Generate unique numeric barcode for POS system
        uniqueData = BarcodeGenerator.generateNumeric(12); // 12 digits for EAN-13 compatible
        
        // Generate CODE 128 barcode (standard POS barcode format)
        Code128Writer barcodeWriter = new Code128Writer();
        BitMatrix bitMatrix = barcodeWriter.encode(
                uniqueData,
                BarcodeFormat.CODE_128,
                300,  // width
                80    // height (barcode height)
        );
        
        bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        Image image = SwingFXUtils.toFXImage(bufferedImage, null);
        barcodeImage.setImage(image);
    }

    public void setDetails(int code, String description, Stage stage,
                           boolean state, ProductDetailTm tm) {
        this.stage = stage;

        if (state) {
            try {
                ProductDetail productDetail = productDetailService.findProductDetail(tm.getCode());

                if (productDetail != null) {
                    txtQty.setText(String.valueOf(productDetail.getQtyOnHand()));
                    txtBuyingPrice.setText(String.valueOf(productDetail.getBuyingPrice()));
                    txtSellingPrice.setText(String.valueOf(productDetail.getSellingPrice()));
                    txtShowPrice.setText(String.valueOf(productDetail.getShowPrice()));
                    rBtnYes.setSelected(productDetail.isDiscountAvailability());
                    uniqueData = productDetail.getCode();

                    // Decode barcode image from Base64
                    byte[] data = Base64.getDecoder().decode(productDetail.getBarcode());
                    barcodeImage.setImage(
                            new Image(new ByteArrayInputStream(data))
                    );

                } else {
                    stage.close();
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                setBarcode();
            } catch (WriterException e) {
                throw new RuntimeException(e);
            }
        }

        txtProductCode.setText(String.valueOf(code));
        txtSelectedProdDescription.setText(description);
    }

    @Transactional
    public void saveBatch(ActionEvent actionEvent) throws IOException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bufferedImage, "png", baos);
            byte[] arr = baos.toByteArray();

            ProductDetail productDetail = new ProductDetail();
            productDetail.setCode(uniqueData);
            productDetail.setBarcode(Base64.getEncoder().encodeToString(arr));
            productDetail.setQtyOnHand(Integer.parseInt(txtQty.getText()));
            productDetail.setSellingPrice(Double.parseDouble(txtSellingPrice.getText()));
            productDetail.setShowPrice(Double.parseDouble(txtShowPrice.getText()));
            productDetail.setBuyingPrice(Double.parseDouble(txtBuyingPrice.getText()));
            productDetail.setProductCode(Integer.parseInt(txtProductCode.getText()));
            productDetail.setDiscountAvailability(rBtnYes.isSelected());

            productDetailService.saveProductDetail(productDetail);
            
            new Alert(Alert.AlertType.CONFIRMATION, "Batch Saved!").show();
            Thread.sleep(3000);
            this.stage.close();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Error saving batch: " + e.getMessage()).show();
        }
    }
}
