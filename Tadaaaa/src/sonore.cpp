
// Ce fichier contient différentes fonctions gérant le retour sonore

#include <Arduino.h>
#include <M5Stack.h>
#include "BluetoothA2DPSource.h"
#include "BluetoothA2DPCommon.h"
#include "sensors.h"

// DONNEES RELATIVES AU BLUETOOTH AVEC CASQUE -------------------------------------
BluetoothA2DPSource a2dp_source;

// DONNEES RELATIVES AUX SONS ----------------------------------------------------- 

extern const unsigned char connectionCheck_wav[];
extern const unsigned int connectionCheck_wav_len;

extern const unsigned char warning_wav[];
extern const unsigned int warning_wav_len;

extern const unsigned char danger_wav[];
extern const unsigned int danger_wav_len;

SoundData *connection = new TwoChannelSoundData((Frame *)connectionCheck_wav, connectionCheck_wav_len / 4);
SoundData *warning = new TwoChannelSoundData((Frame *)warning_wav, warning_wav_len / 4);
SoundData *danger = new TwoChannelSoundData((Frame *)danger_wav, danger_wav_len / 4);

// IMPORTS DEPUIS SENSORS --------------------------------------------------------

extern int sensorValues[NBBOARD][6];
extern int limites[2][2];
extern bool modeDistance;

// MODE SONORE------------------------------
// Initialisé à 0 (désactivé)
bool modeSonore = 0;

// FONCTIONS ---------------------------------------------------------------------

// Lance connexion si activation
void activerSonore(){
    M5.Lcd.setCursor(0, 0);

    // Connexion au casque grâce à son nom
    a2dp_source.start("OpenMove by Shokz");

    // Mettre à jour le volume pour les sons
    a2dp_source.set_volume(50);

    // Envoi d'un son "Connected to M5Stack" pour confirmer la connexion Bluetooth
    //a2dp_source.write_data(connection);
}

// Méthode qui permet d'envoyer le son de danger
void sendDanger()
{
    a2dp_source.write_data(danger);
}

// Méthode qui permet d'envoyer le son d'alerte
void sendWarning()
{
    a2dp_source.write_data(warning);
}

/**
 * Cette fonction est celle qui permet d'envoyer un signal sonore ou non en fonction de la proximité entre un capteur et le fauteuil roulant électrique
 *
 * @param value permet d'indiquer la valeur (distance) reçue par un capteur précis
 */
void retourSonore()
{
    int value0, value1, value4, value5;

    for (int j = 0; j < 6; j++)
    {
        // value0 = sensorValues[0][j];
        // value1 = sensorValues[1][j];
        // value4 = sensorValues[4][j];
        value5 = sensorValues[5][1];

        if (/*value0 < DANGER || value1 < DANGER ||*/ value5 < limites[modeDistance][DANGER])
        {
            M5.Lcd.print("danger");
            sendDanger();
        }

        // Envoi du son aigu avec value inférieure à 60 cm (WARNING)
        else if (/*value0 < WARNING && value0 >= DANGER || value1 < WARNING && value1 >= DANGER ||*/ value5 < limites[modeDistance][ALERTE] && value5 >= limites[modeDistance][DANGER])
        {
            M5.Lcd.print("warning");
            sendWarning();
        }
    }

    /* for (int i = 0; i < NBBOARD; i++)
    {
        for (int j = 0; j < 6; j++)
        {
            value = sensorValues[i][j];

            // Envoi du son aigu avec value inférieure à 30 cm (DANGER)

        }
    }*/
}

/**
 * Permet de noter les numéros des capteurs qui fonctionnent
 */
void testAutomatiqueSonore()
{
    M5.Lcd.clear();

    for (int i = 0; i < NBBOARD; i++)
    {
        for (int j = 0; j < 6; j++)
        {
            M5.Lcd.print(i + " : ");
            if (sensorValues[i][j] != 0)
            {
                M5.Lcd.print(j + " ");
            }
            M5.Lcd.printf(" ");
        }
    }
}