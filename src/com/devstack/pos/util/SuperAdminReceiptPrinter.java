package com.devstack.pos.util;

import com.devstack.pos.service.SuperAdminPDFReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for printing super admin receipts
 * Uses separate methods from existing receipt printer
 */
@Component
@RequiredArgsConstructor
public class SuperAdminReceiptPrinter {
    
    private final SuperAdminPDFReportService superAdminPDFReportService;
    
    /**
     * Print super admin receipt as plain text to thermal printer
     * @param orderId Super admin order ID
     * @return true if printing was successful, false otherwise
     */
    public boolean printSuperAdminReceipt(Long orderId) {
        try {
            // Generate plain text receipt
            String receiptText = superAdminPDFReportService.generateSuperAdminPlainTextReceipt(orderId);
            
            // Print using raw text method with ESC/POS commands
            return printRawText(receiptText);
            
        } catch (Exception e) {
            System.err.println("Error printing super admin receipt: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Print raw text to default printer (thermal printer) using ESC/POS commands
     * @param text Text to print
     * @return true if printing was successful, false otherwise
     */
    private boolean printRawText(String text) {
        try {
            // Find default printer
            PrintService printer = PrintServiceLookup.lookupDefaultPrintService();
            
            if (printer == null) {
                System.err.println("No printer found for super admin receipt.");
                return false;
            }
            
            System.out.println("Printing Super Admin Receipt to: " + printer.getName());
            
            // Prepare ESC/POS formatted data
            byte[] escposData = formatReceiptAsESCPOS(text);
            
            // Create print job with raw data
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(escposData, flavor, null);
            DocPrintJob job = printer.createPrintJob();
            PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
            
            // Print the raw data
            job.print(doc, attributes);
            
            System.out.println("Super Admin Receipt sent to thermal printer successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("Error printing super admin raw text: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Formats receipt text with ESC/POS commands for thermal printer
     * @param receiptText Plain text receipt content
     * @return ESC/POS formatted byte array
     */
    private byte[] formatReceiptAsESCPOS(String receiptText) {
        try {
            List<Byte> data = new ArrayList<>();
            
            // ESC/POS initialization
            data.add((byte) 0x1B); // ESC
            data.add((byte) 0x40); // @  - Initialize printer
            
            // Set character code table to PC437 (USA, Standard Europe)
            data.add((byte) 0x1B); // ESC
            data.add((byte) 0x74); // t
            data.add((byte) 0x00); // 0 = PC437
            
            // Convert receipt text to bytes (using US-ASCII for thermal printer compatibility)
            byte[] textBytes = receiptText.getBytes(StandardCharsets.US_ASCII);
            for (byte b : textBytes) {
                data.add(b);
            }
            
            // Feed and cut
            data.add((byte) 0x0A); // LF - line feed
            data.add((byte) 0x0A); // LF
            data.add((byte) 0x0A); // LF
            data.add((byte) 0x1B); // ESC
            data.add((byte) 0x64); // d
            data.add((byte) 0x02); // feed 2 lines
            data.add((byte) 0x1D); // GS
            data.add((byte) 0x56); // V
            data.add((byte) 0x01); // partial cut
            
            // Convert List<Byte> to byte[]
            byte[] result = new byte[data.size()];
            for (int i = 0; i < data.size(); i++) {
                result[i] = data.get(i);
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("Error formatting Super Admin ESC/POS data: " + e.getMessage());
            e.printStackTrace();
            return new byte[0];
        }
    }
    
}

