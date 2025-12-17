package com.example.esim;

import androidx.room.Dao;
import androidx.room.Delete; // Импорт важен
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface EsimProfileDao {
    @Insert
    void insert(EsimProfile profile);

    @Delete
    void delete(EsimProfile profile); // <-- Новый метод

    @Query("SELECT * FROM esim_profiles")
    List<EsimProfile> getAllProfiles();

    @Query("SELECT * FROM esim_profiles WHERE id = :id")
    EsimProfile getProfileById(int id);
}