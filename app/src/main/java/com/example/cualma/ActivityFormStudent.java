package com.example.cualma;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ActivityFormStudent extends AppCompatActivity {

    private EditText codeEt, nameEt, lastnameEt, carnetEt, careerEt;
    private Button saveButton;
    private ImageButton backButton;
    private TextView titleTextView;
    private int studentId = -1; // -1 indica nuevo registro

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_form_student);

        initializeViews();

        // Verificar si es edición
        if (getIntent().hasExtra("STUDENT_ID")) {
            studentId = getIntent().getIntExtra("STUDENT_ID", -1);
            loadStudentData();
            titleTextView.setText(R.string.form_student_edit_title);
        }

        setupListeners();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initializeViews() {
        codeEt = findViewById(R.id.studentCodeEditText);
        nameEt = findViewById(R.id.studentNameEditText);
        lastnameEt = findViewById(R.id.studentLastnameEditText);
        carnetEt = findViewById(R.id.studentCarnetEditText);
        careerEt = findViewById(R.id.studentCareerEditText);
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);
        titleTextView = findViewById(R.id.titleTextView);
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> validateAndSave());
    }

    private void loadStudentData() {
        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this, "bd_cualma", null, 1);
        SQLiteDatabase db = admin.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT codigo_estudiante, nombres, apellidos, carnet, carrera FROM estudiantes WHERE id = ?", new String[]{String.valueOf(studentId)});

        if (cursor.moveToFirst()) {
            codeEt.setText(cursor.getString(0));
            nameEt.setText(cursor.getString(1));
            lastnameEt.setText(cursor.getString(2));
            carnetEt.setText(cursor.getString(3));
            careerEt.setText(cursor.getString(4));
        }
        cursor.close();
        db.close();
    }

    private void validateAndSave() {
        String code = codeEt.getText().toString().trim();
        String name = nameEt.getText().toString().trim();
        String lastname = lastnameEt.getText().toString().trim();
        String carnet = carnetEt.getText().toString().trim();
        String career = careerEt.getText().toString().trim();

        // Validaciones básicas
        if (code.isEmpty() || name.isEmpty() || lastname.isEmpty() || carnet.isEmpty() || career.isEmpty()) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        if (carnet.length() != 8) {
            Toast.makeText(this, "El carnet debe tener exactamente 8 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        AdminSQLiteOpenHelper admin = new AdminSQLiteOpenHelper(this, "bd_cualma", null, 1);
        SQLiteDatabase db = admin.getReadableDatabase();

        // Validación de duplicados (Carnet o Código)
        String queryCheck = "SELECT id FROM estudiantes WHERE (carnet = ? OR codigo_estudiante = ?)";
        String[] argsCheck = new String[]{carnet, code};

        if (studentId != -1) {
            // Si es edición, excluimos el ID actual de la búsqueda de duplicados
            queryCheck += " AND id != ?";
            argsCheck = new String[]{carnet, code, String.valueOf(studentId)};
        }

        Cursor cursor = db.rawQuery(queryCheck, argsCheck);
        if (cursor.getCount() > 0) {
            Toast.makeText(this, "Error: El carnet o código de estudiante ya existe", Toast.LENGTH_LONG).show();
            cursor.close();
            db.close();
            return;
        }
        cursor.close();

        // Guardar
        db = admin.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("codigo_estudiante", code);
        values.put("nombres", name);
        values.put("apellidos", lastname);
        values.put("carnet", carnet);
        values.put("carrera", career);

        long result;
        if (studentId == -1) {
            result = db.insert("estudiantes", null, values);
            if (result != -1) Toast.makeText(this, "Estudiante registrado", Toast.LENGTH_SHORT).show();
        } else {
            result = db.update("estudiantes", values, "id = ?", new String[]{String.valueOf(studentId)});
            if (result > 0) Toast.makeText(this, "Estudiante actualizado", Toast.LENGTH_SHORT).show();
        }

        db.close();

        if (result != -1 && result != 0) {
            setResult(Activity.RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Error al guardar en la base de datos", Toast.LENGTH_SHORT).show();
        }
    }
}