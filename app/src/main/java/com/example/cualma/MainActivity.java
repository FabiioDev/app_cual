package com.example.cualma;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private LinearLayout studentsContainer;
    private LinearLayout emptyStateLayout;
    private EditText searchEditText;
    private ImageView searchIconImageView;
    private LinearLayout addButton;
    private FloatingActionButton exportFab; // Nuevo botón

    // Launcher para refrescar la lista si agregamos/editamos algo
    private final ActivityResultLauncher<Intent> formLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> loadStudents(searchEditText.getText().toString())
    );

    // Launcher para crear el archivo de respaldo
    private final ActivityResultLauncher<Intent> createDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        performExport(uri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupListeners();
        loadStudents(""); // Carga inicial

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        studentsContainer = findViewById(R.id.studentsContainer);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        searchEditText = findViewById(R.id.searchEditText);
        searchIconImageView = findViewById(R.id.searchIconImageView);
        addButton = findViewById(R.id.addButton);
        exportFab = findViewById(R.id.exportFab); // Inicializar FAB
    }

    private void setupListeners() {
        // Navegación a Agregar Estudiante
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ActivityFormStudent.class);
            formLauncher.launch(intent);
        });

        // Listener para Exportar Base de Datos
        exportFab.setOnClickListener(v -> initiateExport());

        // Búsqueda al presionar el ícono
        searchIconImageView.setOnClickListener(v -> {
            loadStudents(searchEditText.getText().toString());
        });

        // Búsqueda al presionar Enter en el teclado
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                loadStudents(searchEditText.getText().toString());
                return true;
            }
            return false;
        });
    }

    // --- LÓGICA DE EXPORTACIÓN ---

    private void initiateExport() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/x-sqlite3"); // Tipo MIME para SQLite
        intent.putExtra(Intent.EXTRA_TITLE, "backup_cualma.db"); // Nombre sugerido
        createDocumentLauncher.launch(intent);
    }

    private void performExport(Uri uri) {
        try {
            File dbFile = getDatabasePath("bd_cualma");

            if (!dbFile.exists()) {
                Toast.makeText(this, "No existe base de datos para exportar", Toast.LENGTH_SHORT).show();
                return;
            }

            try (FileInputStream in = new FileInputStream(dbFile);
                 ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
                 FileOutputStream out = new FileOutputStream(pfd.getFileDescriptor())) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }

                Toast.makeText(this, "Base de datos exportada con éxito", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al exportar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadStudents(String query) {
        studentsContainer.removeAllViews();
        emptyStateLayout.setVisibility(View.GONE);

        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this, "bd_cualma", null, 1);
        SQLiteDatabase db = admin.getReadableDatabase();

        boolean resultsFound = false;

        // NIVEL 1: BÚSQUEDA DE ESTUDIANTES
        String sqlStudents = "SELECT id, nombres, apellidos, codigo_estudiante, carrera FROM estudiantes";
        String[] argsStudents = null;

        if (!query.trim().isEmpty()) {
            sqlStudents += " WHERE nombres LIKE ? OR apellidos LIKE ?";
            String param = "%" + query.trim() + "%";
            argsStudents = new String[]{param, param};
        }

        Cursor cursorStudents = db.rawQuery(sqlStudents, argsStudents);

        if (cursorStudents.getCount() > 0) {
            resultsFound = true;
            while (cursorStudents.moveToNext()) {
                addStudentCard(
                        cursorStudents.getInt(0),
                        cursorStudents.getString(1) + " " + cursorStudents.getString(2),
                        cursorStudents.getString(3),
                        cursorStudents.getString(4)
                );
            }
        }
        cursorStudents.close();

        // NIVEL 2: BÚSQUEDA DE MATERIAS
        if (!resultsFound && !query.trim().isEmpty()) {
            String sqlSubjects = "SELECT id, nombre, codigo_materia, hora_inicio, hora_fin, aula, nombre_docente, id_estudiante FROM materias " +
                    "WHERE codigo_materia LIKE ? OR nombre LIKE ? OR nombre_docente LIKE ?";
            String param = "%" + query.trim() + "%";
            String[] argsSubjects = new String[]{param, param, param};

            Cursor cursorSubjects = db.rawQuery(sqlSubjects, argsSubjects);

            if (cursorSubjects.getCount() > 0) {
                resultsFound = true;
                while (cursorSubjects.moveToNext()) {
                    addSubjectCardToMain(
                            cursorSubjects.getInt(0),
                            cursorSubjects.getString(1),
                            cursorSubjects.getString(2),
                            cursorSubjects.getString(3) + " - " + cursorSubjects.getString(4),
                            cursorSubjects.getString(5),
                            cursorSubjects.getString(6),
                            cursorSubjects.getInt(7)
                    );
                }
            }
            cursorSubjects.close();
        }

        db.close();

        if (!resultsFound) {
            emptyStateLayout.setVisibility(View.VISIBLE);
        }
    }

    private void addStudentCard(int id, String name, String code, String career) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_student_card, studentsContainer, false);

        TextView nameTv = cardView.findViewById(R.id.studentNameTextView);
        TextView codeTv = cardView.findViewById(R.id.studentCodeTextView);
        TextView careerTv = cardView.findViewById(R.id.studentCareerTextView);

        nameTv.setText(name);
        codeTv.setText(code);
        careerTv.setText(career);

        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ActivityDetailStudent.class);
            intent.putExtra("STUDENT_ID", id);
            formLauncher.launch(intent);
        });

        studentsContainer.addView(cardView);
    }

    private void addSubjectCardToMain(int idMateria, String nombre, String codigo, String horario, String aula, String docente, int idEstudiante) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_subject_card, studentsContainer, false);

        TextView nameTv = view.findViewById(R.id.subjectNameTextView);
        TextView codeTv = view.findViewById(R.id.subjectCodeTextView);
        TextView scheduleTv = view.findViewById(R.id.subjectScheduleTextView);
        TextView classroomTv = view.findViewById(R.id.subjectClassroomTextView);
        TextView teacherTv = view.findViewById(R.id.subjectTeacherTextView);

        nameTv.setText(nombre);
        codeTv.setText(codigo);
        scheduleTv.setText(horario);
        classroomTv.setText(aula);
        teacherTv.setText(docente);

        view.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ActivityFormSubject.class);
            intent.putExtra("SUBJECT_ID", idMateria);
            intent.putExtra("STUDENT_ID", idEstudiante);
            formLauncher.launch(intent);
        });

        studentsContainer.addView(view);
    }
}