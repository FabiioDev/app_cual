package com.example.cualma;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ActivityDetailSubject extends AppCompatActivity {

    private TextView subjectNameTv, subjectCodeTv, scheduleTv, classroomTv, teacherTv;
    private LinearLayout editButton, deleteButton, homeButton;
    private ImageButton backButton;

    private int subjectId = -1;
    private int studentId = -1;

    // Launcher para recargar los datos si se edita la materia desde esta pantalla
    private final ActivityResultLauncher<Intent> editLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    loadSubjectData(); // Recargar datos actualizados
                    setResult(Activity.RESULT_OK); // Propagar el resultado a la pantalla anterior
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail_subject);

        // Obtener IDs
        subjectId = getIntent().getIntExtra("SUBJECT_ID", -1);
        studentId = getIntent().getIntExtra("STUDENT_ID", -1);

        if (subjectId == -1) {
            Toast.makeText(this, "Error: Asignatura no encontrada", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupListeners();
        loadSubjectData();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        // Asegúrate de que estos IDs coincidan con activity_detail_subject.xml
        subjectNameTv = findViewById(R.id.subjectNameTextView);
        subjectCodeTv = findViewById(R.id.subjectCodeTextView);
        scheduleTv = findViewById(R.id.scheduleTextView);
        classroomTv = findViewById(R.id.classroomTextView);
        teacherTv = findViewById(R.id.teacherNameTextView);

        editButton = findViewById(R.id.editButton);
        deleteButton = findViewById(R.id.deleteButton);
        homeButton = findViewById(R.id.homeButton);
        backButton = findViewById(R.id.backButton);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        // Redirigir al formulario para EDICIÓN
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ActivityFormSubject.class);
            intent.putExtra("SUBJECT_ID", subjectId);
            intent.putExtra("STUDENT_ID", studentId);
            editLauncher.launch(intent);
        });

        deleteButton.setOnClickListener(v -> confirmDelete());
    }

    private void loadSubjectData() {
        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this, "bd_cualma", null, 1);
        SQLiteDatabase db = admin.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT nombre, codigo_materia, hora_inicio, hora_fin, aula, nombre_docente FROM materias WHERE id = ?", new String[]{String.valueOf(subjectId)});

        if (cursor.moveToFirst()) {
            subjectNameTv.setText(cursor.getString(0));
            subjectCodeTv.setText(cursor.getString(1));
            scheduleTv.setText(cursor.getString(2) + " - " + cursor.getString(3));
            classroomTv.setText(cursor.getString(4));
            teacherTv.setText(cursor.getString(5));
        } else {
            Toast.makeText(this, "La materia no existe", Toast.LENGTH_SHORT).show();
            finish();
        }
        cursor.close();
        db.close();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Asignatura")
                .setMessage("¿Estás seguro de eliminar esta asignatura?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this, "bd_cualma", null, 1);
                    SQLiteDatabase db = admin.getWritableDatabase();
                    int result = db.delete("materias", "id = ?", new String[]{String.valueOf(subjectId)});
                    db.close();

                    if (result > 0) {
                        Toast.makeText(this, "Asignatura eliminada", Toast.LENGTH_SHORT).show();
                        setResult(Activity.RESULT_OK); // Avisar a la actividad anterior (Detalle Estudiante)
                        finish();
                    } else {
                        Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}