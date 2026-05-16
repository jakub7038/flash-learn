package com.flashlearn.backend.report;

import com.flashlearn.backend.deck.DeckService;
import com.flashlearn.backend.exception.DeckNotFoundException;
import com.flashlearn.backend.exception.ResourceAccessDeniedException;
import com.flashlearn.backend.model.Deck;
import com.flashlearn.backend.model.Flashcard;
import com.flashlearn.backend.model.User;
import com.flashlearn.backend.repository.DeckRepository;
import com.flashlearn.backend.repository.UserRepository;
import com.flashlearn.backend.stats.StatsResponse;
import com.flashlearn.backend.stats.StatsService;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfReportService {

    private final StatsService statsService;
    private final DeckRepository deckRepository;
    private final UserRepository userRepository;

    // Raport statystyk
    @Transactional(readOnly = true)
    public byte[] buildStatsPdf() {
        StatsResponse stats = statsService.getStats();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = createDocument(baos);
            PdfFont regular = buildFont();
            PdfFont bold    = buildFontBold();

            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            addHeader(doc, "Raport statystyk", email, bold, regular);

            Table table = new Table(UnitValue.createPercentArray(new float[]{3f, 2f}))
                    .useAllAvailableWidth();

            addTableHeader(table, bold, "Parametr", "Wartosc");
            addTableRow(table, regular, "Lacznie ocenionych fiszek (30 dni)",
                        String.valueOf(stats.getTotalReviewed()));
            addTableRow(table, regular, "Poprawne (rating=2)",
                        String.valueOf(stats.getCorrectAnswers()));
            addTableRow(table, regular, "Trudne (rating=1)",
                        String.valueOf(stats.getHardAnswers()));
            addTableRow(table, regular, "Nieznane (rating=0)",
                        String.valueOf(stats.getWrongAnswers()));
            addTableRow(table, regular, "Aktualny streak (dni)",
                        String.valueOf(stats.getCurrentStreak()));
            addTableRow(table, regular, "Najdluzszy streak (dni)",
                        String.valueOf(stats.getLongestStreak()));

            doc.add(table);
            addFooter(doc, regular, "");
            doc.close();
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Blad generowania PDF statystyk", e);
        }
    }

    // Eksport talii fiszek 
    @Transactional(readOnly = true)
    public byte[] buildDeckPdf(Long deckId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Deck deck = deckRepository.findByIdWithFlashcards(deckId)
                .orElseThrow(() -> new DeckNotFoundException(deckId));

        if (!deck.getOwner().getId().equals(user.getId())) {
            throw new ResourceAccessDeniedException("Access denied: deck id=" + deckId);
        }

        List<Flashcard> flashcards = deck.getFlashcards();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = createDocument(baos);
            PdfFont regular = buildFont();
            PdfFont bold    = buildFontBold();

            addHeader(doc, "Talia: " + deck.getTitle(), email, bold, regular);

            Table table = new Table(UnitValue.createPercentArray(new float[]{0.5f, 3f, 3f}))
                    .useAllAvailableWidth();

            addTableHeader(table, bold, "Nr", "Pytanie", "Odpowiedz");

            for (int i = 0; i < flashcards.size(); i++) {
                Flashcard f = flashcards.get(i);
                addTableRow(table, regular,
                        String.valueOf(i + 1),
                        f.getQuestion(),
                        f.getAnswer());
            }

            doc.add(table);
            addFooter(doc, regular, "Liczba fiszek: " + flashcards.size() + "  |  ");
            doc.close();
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Blad generowania PDF talii", e);
        }
    }

    // Helpers 
    private Document createDocument(ByteArrayOutputStream baos) throws IOException {
        return new Document(new PdfDocument(new PdfWriter(baos)), PageSize.A4);
    }

    private void addHeader(Document doc, String title, String email,
                           PdfFont bold, PdfFont regular) {
        doc.add(new Paragraph(title)
                .setFont(bold).setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph("Uzytkownik: " + email)
                .setFont(regular).setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(ColorConstants.DARK_GRAY));
        doc.add(new Paragraph("\n"));
    }

    private void addTableHeader(Table table, PdfFont bold, String... headers) {
        for (String h : headers) {
            table.addHeaderCell(
                new Cell().add(new Paragraph(h).setFont(bold).setFontSize(11))
                          .setBackgroundColor(ColorConstants.LIGHT_GRAY));
        }
    }

    private void addTableRow(Table table, PdfFont regular, String... values) {
        for (String v : values) {
            table.addCell(
                new Cell().add(new Paragraph(v).setFont(regular).setFontSize(10)));
        }
    }

    private void addFooter(Document doc, PdfFont regular, String prefix) {
        doc.add(new Paragraph("\n" + prefix + "Wygenerowano: " + LocalDate.now())
                .setFont(regular).setFontSize(9)
                .setFontColor(ColorConstants.GRAY));
    }

    /*
     * POLSKIE ZNAKI: wrzuc DejaVuSans.ttf do src/main/resources/fonts/
     * i zamien buildFont() na:
     *
     *   return PdfFontFactory.createFont(
     *       "fonts/DejaVuSans.ttf",
     *       PdfEncodings.IDENTITY_H,
     *       EmbeddingStrategy.FORCE_EMBEDDED);
     */
    private PdfFont buildFont() throws IOException {
        return PdfFontFactory.createFont(StandardFonts.HELVETICA);
    }

    private PdfFont buildFontBold() throws IOException {
        return PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
    }
}