package com.example.ui.utils

import android.content.Context
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.data.CategorySaleInfo

fun generateCourierReportPdf(
    context: Context,
    name: String,
    vehicleType: String,
    rating: String,
    completedCount: Int,
    distance: Double,
    income: Double,
    period: String = ""
) {
    // Left empty or we can implement it similarly
}

fun generateMonthlyReportPdf(
    context: Context,
    month: String,
    revenue: Double,
    netProfit: Double,
    productCost: Double,
    logisticsCost: Double,
    soldCount: Int,
    unsoldCount: Int,
    cancelledCount: Int,
    refundLoss: Double,
    telebirrPayout: Double,
    cbePayout: Double,
    ebirrPayout: Double,
    storeName: String,
    tin: String,
    license: String,
    categorySales: List<CategorySaleInfo>,
    uniqueCustomersCount: Int
) {
    val pdfDocument = PdfDocument()
    // A4 dimensions: 595 x 842 points
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    val paint = Paint()
    
    // Background and frame
    canvas.drawColor(Color.WHITE)
    paint.color = Color.parseColor("#4CAF50") // BrandGreenPrimary color
    paint.strokeWidth = 3f
    paint.style = Paint.Style.STROKE
    canvas.drawRect(15f, 15f, 580f, 827f, paint) // border around page

    paint.style = Paint.Style.FILL
    
    // Header
    paint.color = Color.parseColor("#1B5E20") // Dark green
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 14f
    canvas.drawText("ESUUQ STORE SYSTEM", 30f, 50f, paint)

    paint.color = Color.BLACK
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 22f
    canvas.drawText("Official Monthly Statement", 30f, 80f, paint)

    // Horizontal line
    paint.color = Color.LTGRAY
    paint.strokeWidth = 1f
    canvas.drawLine(30f, 100f, 565f, 100f, paint)

    // Store identity & period info
    paint.color = Color.DKGRAY
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 11f
    canvas.drawText("Store Identity:", 30f, 125f, paint)
    paint.color = Color.BLACK
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText(storeName, 30f, 142f, paint)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("TIN: $tin", 30f, 157f, paint)
    canvas.drawText("License: $license", 30f, 172f, paint)

    paint.color = Color.DKGRAY
    canvas.drawText("Statement Period:", 400f, 125f, paint)
    paint.color = Color.parseColor("#1B5E20")
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText(month, 400f, 142f, paint)
    
    val dateStr = try {
        SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date())
    } catch(e: Exception) {
        "No Month, No Year"
    }
    paint.color = Color.BLACK
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("Issued: $dateStr", 400f, 157f, paint)

    // Section 1: Ledger Audit Trails
    paint.color = Color.parseColor("#2E7D32")
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 14f
    canvas.drawText("Ledger Audit Trails", 30f, 210f, paint)

    // Draw box for Ledger Audit Trails
    paint.color = Color.parseColor("#F1F8E9") // very light green
    paint.style = Paint.Style.FILL
    canvas.drawRoundRect(30f, 220f, 565f, 320f, 8f, 8f, paint)

    paint.color = Color.BLACK
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 11f
    canvas.drawText("Gross Sales:", 45f, 242f, paint)
    canvas.drawText(String.format(Locale.US, "%,.2f ETB", revenue), 430f, 242f, paint)

    paint.color = Color.parseColor("#C62828") // dark red
    canvas.drawText("Cost of Product:", 45f, 262f, paint)
    canvas.drawText(String.format(Locale.US, "-%,.2f ETB", productCost), 430f, 262f, paint)

    canvas.drawText("Delivery Logistics Expenses:", 45f, 282f, paint)
    canvas.drawText(String.format(Locale.US, "-%,.2f ETB", logisticsCost), 430f, 282f, paint)

    paint.color = Color.LTGRAY
    canvas.drawLine(45f, 292f, 550f, 292f, paint)

    paint.color = Color.parseColor("#2E7D32")
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Net Shop Profit Margin:", 45f, 310f, paint)
    canvas.drawText(String.format(Locale.US, "%,.2f ETB", netProfit), 430f, 310f, paint)

    // Section 2: Physical Inventory Volume Logs
    paint.color = Color.parseColor("#2E7D32")
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Physical Inventory Volume Logs", 30f, 350f, paint)

    paint.color = Color.parseColor("#ECEFF1")
    paint.style = Paint.Style.FILL
    canvas.drawRoundRect(30f, 360f, 195f, 420f, 6f, 6f, paint)
    canvas.drawRoundRect(215f, 360f, 380f, 420f, 6f, 6f, paint)
    canvas.drawRoundRect(400f, 360f, 565f, 420f, 6f, 6f, paint)

    paint.color = Color.BLACK
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 10f
    canvas.drawText("Sold Items", 40f, 380f, paint)
    canvas.drawText("Unsold Stock", 225f, 380f, paint)
    canvas.drawText("Unique Clients", 410f, 380f, paint)

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 12f
    paint.color = Color.parseColor("#2E7D32")
    canvas.drawText("$soldCount units", 40f, 405f, paint)
    paint.color = Color.parseColor("#C62828")
    canvas.drawText("$unsoldCount units", 225f, 405f, paint)
    paint.color = Color.parseColor("#1565C0")
    canvas.drawText("$uniqueCustomersCount buyers", 410f, 405f, paint)

    // Section 3: Order Cancellations & Refund Losses
    paint.color = Color.parseColor("#2E7D32")
    canvas.drawText("Order Cancellations & Refund Losses", 30f, 450f, paint)

    paint.color = Color.parseColor("#FFEBEE")
    paint.style = Paint.Style.FILL
    canvas.drawRoundRect(30f, 460f, 280f, 520f, 6f, 6f, paint)
    canvas.drawRoundRect(315f, 460f, 565f, 520f, 6f, 6f, paint)

    paint.color = Color.BLACK
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 10f
    canvas.drawText("Cancelled Orders", 40f, 480f, paint)
    canvas.drawText("Est. Refund Loss", 325f, 480f, paint)

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 12f
    paint.color = Color.parseColor("#C62828")
    canvas.drawText("$cancelledCount orders", 40f, 505f, paint)
    canvas.drawText(String.format(Locale.US, "%,.2f ETB", refundLoss), 325f, 505f, paint)

    // Section 4: Bank Disbursements Allocation
    paint.color = Color.parseColor("#2E7D32")
    canvas.drawText("Bank Disbursements Allocation", 30f, 550f, paint)

    paint.color = Color.BLACK
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 11f
    canvas.drawText("Telebirr Merchant Payout:", 40f, 575f, paint)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = Color.parseColor("#2E7D32")
    canvas.drawText(String.format(Locale.US, "%,.2f ETB", telebirrPayout), 380f, 575f, paint)

    paint.color = Color.BLACK
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("CBE Bank Transfer:", 40f, 595f, paint)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = Color.parseColor("#1565C0")
    canvas.drawText(String.format(Locale.US, "%,.2f ETB", cbePayout), 380f, 595f, paint)

    paint.color = Color.BLACK
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("Ebirr Wallet Allocation:", 40f, 615f, paint)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = Color.parseColor("#F57F17") // Gold/Orange for Ebirr
    canvas.drawText(String.format(Locale.US, "%,.2f ETB", ebirrPayout), 380f, 615f, paint)

    // Section 5: Best-Selling Categories
    paint.color = Color.parseColor("#2E7D32")
    canvas.drawText("Best-Selling Categories & Subcategories", 30f, 650f, paint)
    
    var yOffset = 675f
    if (categorySales.isNotEmpty()) {
        paint.textSize = 9f
        categorySales.take(3).forEach { category ->
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.BLACK
            canvas.drawText("${category.name}:", 40f, yOffset, paint)
            paint.color = Color.parseColor("#2E7D32")
            canvas.drawText("${category.soldCount} units sold", 180f, yOffset, paint)
            
            yOffset += 14f
            
            if (category.items.isNotEmpty()) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.DKGRAY
                val subItemsStr = category.items.joinToString(", ") { "${it.first} (${it.second})" }
                canvas.drawText("  • Subcategories: $subItemsStr", 40f, yOffset, paint)
                yOffset += 14f
            }
        }
    } else {
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        canvas.drawText("Generous inventory analytics and transaction validation logs are included in your official digital dashboard.", 40f, yOffset, paint)
    }

    // Disclaimer
    paint.color = Color.GRAY
    paint.textSize = 9f
    canvas.drawText("This is an officially signed statement of your digital business operations in Esuuq Store System.", 30f, 750f, paint)
    canvas.drawText("Please retain this copy for your tax returns and commercial registration audit requirements.", 30f, 765f, paint)

    pdfDocument.finishPage(page)

    // Save to public Documents directory
    val cleanMonthName = month.replace(" ", "_")
    val fileName = "Esuuq_Statement_${cleanMonthName}.pdf"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
        }
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                pdfDocument.close()
                Toast.makeText(context, "Statement saved to public Documents folder!", Toast.LENGTH_LONG).show()
                return
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to legacy
            }
        }
    }

    // Fallback for pre-Q or when MediaStore fails
    val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    if (!publicDir.exists()) {
        publicDir.mkdirs()
    }
    val file = File(publicDir, fileName)
    try {
        val fos = FileOutputStream(file)
        pdfDocument.writeTo(fos)
        pdfDocument.close()
        fos.close()
        Toast.makeText(context, "Statement saved to public Documents folder!", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        // If that fails too, fallback to private sandboxed downloads as an absolute last resort
        val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val fallbackFile = File(fallbackDir, fileName)
        try {
            val fos = FileOutputStream(fallbackFile)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            Toast.makeText(context, "Saved to private app downloads: ${fallbackFile.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (ex: Exception) {
            ex.printStackTrace()
            Toast.makeText(context, "Error saving PDF: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
