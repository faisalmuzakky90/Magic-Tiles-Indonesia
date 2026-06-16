public class Song {
    public String title, artist, bpmLabel, color, diffLabel;
    public int bpm;
    public int[] pattern;

    public Song(String title, String artist, int bpm, int[] pattern, String color, String diff) {
        this.title = title; this.artist = artist; this.bpm = bpm;
        this.bpmLabel = bpm + " BPM"; this.pattern = pattern;
        this.color = color; this.diffLabel = diff;
    }

    public static Song[] getSongs() {
        return new Song[]{

            // 86 BPM - slow, pattern simpel kiri-kanan bergantian
            new Song("Mungkin di Depan Buram","Idgitaf",86,
                new int[]{
                    0,2, 1,3, 0,2, 1,3,   // intro: kiri-kanan bergantian pelan
                    0,1, 2,3, 2,1, 0,3,   // verse: mengalir
                    1,2, 0,3, 1,2, 0,3,   // pre-chorus
                    0,3, 1,2, 0,3, 1,2,   // chorus: lebih dinamis
                    2,0, 3,1, 2,0, 3,1,   // bridge
                    0,1, 2,3, 1,0, 3,2,   // outro
                },
                "#A78BFA","MUDAH"),

            // 97 BPM - sedang, pattern lebih variatif
            new Song("Sesi Potret","enau & Ari Lesmana",97,
                new int[]{
                    1,3, 0,2, 1,3, 0,2,   // intro
                    0,1, 3,2, 0,1, 3,2,   // verse 1
                    1,0, 2,3, 1,0, 2,3,   // verse 2
                    0,3, 1,2, 3,0, 2,1,   // chorus
                    1,2, 0,3, 2,1, 3,0,   // chorus lanjut
                    0,1, 2,3, 0,2, 1,3,   // bridge
                    3,1, 0,2, 3,1, 0,2,   // outro
                },
                "#F472B6","NORMAL"),

            // 125 BPM - cepat, pattern energik
            new Song("Dan...","Sheila On 7",125,
                new int[]{
                    0,2, 1,3, 0,2, 1,3,   // intro gitar
                    0,1, 2,3, 0,1, 2,3,   // verse naik
                    3,2, 1,0, 3,2, 1,0,   // verse turun
                    0,3, 2,1, 0,3, 2,1,   // pre-chorus
                    0,1, 2,3, 1,2, 0,3,   // chorus naik
                    3,0, 2,1, 3,0, 2,1,   // chorus turun
                    1,3, 0,2, 1,3, 0,2,   // interlude
                    0,2, 3,1, 0,2, 3,1,   // outro
                },
                "#34D399","NORMAL"),

            // 78 BPM - paling lambat, pattern santai
            new Song("Ada Titik di Ujung Doa","Sal Priadi",78,
                new int[]{
                    2,0, 3,1, 2,0, 3,1,   // intro pelan
                    0,2, 1,3, 0,2, 1,3,   // verse 1
                    2,3, 0,1, 2,3, 0,1,   // verse 2
                    0,1, 3,2, 0,1, 3,2,   // chorus
                    1,3, 2,0, 1,3, 2,0,   // chorus lanjut
                    2,0, 1,3, 2,0, 1,3,   // bridge
                    0,3, 1,2, 0,3, 1,2,   // outro
                },
                "#60A5FA","MUDAH"),

            // 115 BPM - cukup cepat, pattern padat
            new Song("Pura Pura","Lyodra",115,
                new int[]{
                    0,3, 1,2, 0,3, 1,2,   // intro upbeat
                    1,0, 3,2, 1,0, 3,2,   // verse 1
                    0,2, 3,1, 0,2, 3,1,   // verse 2
                    0,1, 2,3, 3,2, 1,0,   // pre-chorus naik-turun
                    0,3, 2,1, 0,3, 2,1,   // chorus
                    1,2, 3,0, 1,2, 3,0,   // chorus lanjut
                    0,1, 3,2, 0,2, 1,3,   // bridge
                    0,3, 1,2, 0,3, 1,2,   // outro
                },
                "#FBBF24","SULIT"),

            // 95 BPM - sedang, pattern emosional
            new Song("Jangan Paksa Rindu","Ifan Seventeen",95,
                new int[]{
                    1,2, 0,3, 1,2, 0,3,   // intro
                    0,1, 2,3, 0,1, 2,3,   // verse 1
                    3,2, 1,0, 3,2, 1,0,   // verse 2
                    1,3, 0,2, 1,3, 0,2,   // pre-chorus
                    0,2, 1,3, 2,0, 3,1,   // chorus
                    1,0, 3,2, 1,0, 3,2,   // chorus lanjut
                    2,3, 0,1, 2,3, 0,1,   // bridge
                    1,2, 0,3, 1,2, 0,3,   // outro
                },
                "#FB923C","NORMAL"),

            // ── TAMBAH LAGU BARU DI SINI ──────────────────────────────────
            // new Song("Judul","Artis", BPM,
            //     new int[]{0,1,2,3, 0,2,1,3, ...},"#WARNAHEX","MUDAH/NORMAL/SULIT"),
            // Taruh file WAV di music/7.wav, music/8.wav dst
            // ──────────────────────────────────────────────────────────────
        };
    }
}
