package com.example.cualma;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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

public class ActivityDetailStudent extends AppCompatActivity {

    private TextView nameTv, codeTv, carnetTv, careerTv;
    private LinearLayout subjectsContainer, editButton, deleteButton, homeButton;
    private ImageButton backButton;
    private Button btnAddSubject;
    private int studentId;

    private final ActivityResultLauncher<Intent> updateLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> loadData()
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail_student);

        studentId = getIntent().getIntExtra("STUDENT_ID", -1);
        if (studentId == -1) {
            finish();
            return;
        }

        initializeViews();
        setupListeners();
        loadData();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        nameTv = findViewById(R.id.studentNameTextView);
        codeTv = findViewById(R.id.studentCodeTextView);
        carnetTv = findViewById(R.id.studentCarnetTextView);
        careerTv = findViewById(R.id.studentCareerTextView);
        subjectsContainer = findViewById(R.id.subjectsContainer);

        btnAddSubject = findViewById(R.id.btnAddSubject);
        editButton = findViewById(R.id.editButton);
        deleteButton = findViewById(R.id.deleteButton);
        homeButton = findViewById(R.id.homeButton);
        backButton = findViewById(R.id.backButton);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());

        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ActivityFormStudent.class);
            intent.putExtra("STUDENT_ID", studentId);
            updateLauncher.launch(intent);
        });

        btnAddSubject.setOnClickListener(v -> {
            Intent intent = new Intent(this, ActivityFormSubject.class);
            intent.putExtra("STUDENT_ID", studentId);
            updateLauncher.launch(intent);
        });

        deleteButton.setOnClickListener(v -> confirmDeleteStudent());
    }

    private void loadData() {
        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this, "bd_cualma", null, 1);
        SQLiteDatabase db = admin.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT nombres, apellidos, codigo_estudiante, carnet, carrera FROM estudiantes WHERE id = ?", new String[]{String.valueOf(studentId)});
        if (cursor.moveToFirst()) {
            nameTv.setText(cursor.getString(0) + " " + cursor.getString(1));
            codeTv.setText(cursor.getString(2));
            carnetTv.setText(cursor.getString(3));
            careerTv.setText(cursor.getString(4));
        }
        cursor.close();

        subjectsContainer.removeAllViews();
        Cursor cursorMat = db.rawQuery("SELECT id, nombre, codigo_materia, hora_inicio, hora_fin, aula, nombre_docente FROM materias WHERE id_estudiante = ?", new String[]{String.valueOf(studentId)});

        int subjectCount = 0;
        while (cursorMat.moveToNext()) {
            subjectCount++;
            addSubjectCard(
                    cursorMat.getInt(0),
                    cursorMat.getString(1),
                    cursorMat.getString(2),
                    cursorMat.getString(3) + " - " + cursorMat.getString(4),
                    cursorMat.getString(5),
                    cursorMat.getString(6)
            );
        }
        cursorMat.close();
        db.close();

    }

    private void addSubjectCard(int id, String name, String code, String schedule, String classroom, String teacher) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_subject_card, subjectsContainer, false);

        ((TextView) view.findViewById(R.id.subjectNameTextView)).setText(name);
        ((TextView) view.findViewById(R.id.subjectCodeTextView)).setText(code);
        ((TextView) view.findViewById(R.id.subjectScheduleTextView)).setText(schedule);
        ((TextView) view.findViewById(R.id.subjectClassroomTextView)).setText(classroom);
        ((TextView) view.findViewById(R.id.subjectTeacherTextView)).setText(teacher);

        // CAMBIO PRINCIPAL: Redirigir a ActivityDetailSubject
        view.setOnClickListener(v -> {
            Intent intent = new Intent(this, ActivityDetailSubject.class);
            intent.putExtra("SUBJECT_ID", id);
            intent.putExtra("STUDENT_ID", studentId);
            updateLauncher.launch(intent);
        });

        subjectsContainer.addView(view);
    }

    private void confirmDeleteStudent() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Estudiante")
                .setMessage("¿Estás seguro de eliminar este estudiante y todas sus asignaturas?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this, "bd_cualma", null, 1);
                    SQLiteDatabase db = admin.getWritableDatabase();

                    db.delete("materias", "id_estudiante = ?", new String[]{String.valueOf(studentId)});
                    db.delete("estudiantes", "id = ?", new String[]{String.valueOf(studentId)});

                    db.close();
                    Toast.makeText(this, "Estudiante eliminado", Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK);
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}