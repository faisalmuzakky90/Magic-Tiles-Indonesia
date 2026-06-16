import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;

public class MenuScreen extends JPanel {
    int W, H;
    private JFrame frame;
    private Song[] songs;
    private int sel = 0;
    private Timer timer;
    private float glow = 0;
    private Random rand = new Random();

    private float[] px = new float[150], py = new float[150];
    private float[] pvx = new float[150], pvy = new float[150];
    private float[] plife = new float[150];
    private java.awt.image.BufferedImage cachedCover = null;
    private int cachedSel = -1;

    public MenuScreen(JFrame frame) {
        this.frame = frame;
        this.W = MagicTilesGame.W;
        this.H = MagicTilesGame.H;
        this.songs = Song.getSongs();
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);

        for (int i = 0; i < 150; i++) resetP(i);

        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { handleClick(e.getX(), e.getY()); }
        });
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_LEFT  || k == KeyEvent.VK_UP)   sel = (sel - 1 + songs.length) % songs.length;
                if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_DOWN) sel = (sel + 1) % songs.length;
                if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) startGame();
                if (k == KeyEvent.VK_ESCAPE) System.exit(0);
                repaint();
            }
        });
        setFocusable(true);

        timer = new Timer(16, e -> {
            glow += 0.035f;
            for (int i = 0; i < 150; i++) {
                px[i] += pvx[i]; py[i] += pvy[i];
                pvy[i] -= 0.007f;
                plife[i] -= 0.004f;
                if (plife[i] <= 0) resetP(i);
            }
            repaint();
        });
        timer.start();
    }

    void resetP(int i) {
        px[i] = rand.nextInt(W);
        py[i] = H * 0.75f + rand.nextInt((int)(H * 0.25)); // spawn dari bawah area 
        pvx[i] = (rand.nextFloat() - 0.5f) * 1.2f;
        pvy[i] = -rand.nextFloat() * 4f - 0.8f; // lebih cepat ke atas
        plife[i] = 0.4f + rand.nextFloat() * 0.6f;
    }

    // ── Layout helper — SATU tempat, dipakai paintComponent & handleClick ──
    private int[] calcLayout() {
        // Pakai getWidth()/getHeight() untuk ukuran panel yang actual
        int PW = getWidth()  > 0 ? getWidth()  : W;
        int PH = getHeight() > 0 ? getHeight() : H;
        // [topH, listX, listY, listW, listInnerH, itemH, detailX, detailW, cardY, cardH, pbx, pby, pbw, pbh]
        int topH    = (int)(PH * 0.13);
        int listX   = 8;
        int listY   = topH + 4;
        int listW   = (int)(PW * 0.24);  // panel kanan lebih lebar & centered
        int ITEM_H  = 80;
        int lblH    = Math.max(12, PW/105) + 16;
        int listInH = lblH + songs.length * ITEM_H + 8;
        int detailX = listW + 8;
        int detailW = PW - detailX - 18;
        int cardY   = topH + 6;
        int pbw     = Math.min((int)(detailW * 0.65), 300);
        int pbh     = (int)(PH * 0.058);
        int pby     = PH - pbh - 95;
        int pbx     = detailX + (detailW - pbw) / 2;
        int cardH   = pby - cardY - 16;
        return new int[]{topH, listX, listY, listW, listInH, ITEM_H, detailX, detailW, cardY, cardH, pbx, pby, pbw, pbh};
    }

    void handleClick(int mx, int my) {
        int[] L = calcLayout();
        int listX=L[1], listY=L[2], listW=L[3], ITEM_H=L[5];
        int lblAreaH = Math.max(12, W/105) + 16;
        for (int i = 0; i < songs.length; i++) {
            int iy = listY + lblAreaH + 4 + i * ITEM_H;
            if (mx > listX && mx < listX + listW - 8 && my > iy && my < iy + ITEM_H) {
                sel = i; repaint(); return;
            }
        }
        int pbx=L[10], pby=L[11], pbw=L[12], pbh=L[13];
        if (mx > pbx && mx < pbx + pbw && my > pby && my < pby + pbh) startGame();
    }

    void startGame() {
        timer.stop();
        GameScreen gs = new GameScreen(frame, songs[sel], sel);
        frame.getContentPane().removeAll();
        frame.getContentPane().add(gs);
        frame.setSize(W, H);
        frame.revalidate();
        gs.requestFocusInWindow();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int[] L = calcLayout();
        int topH     = L[0];
        int listX    = L[1], listY = L[2], listW = L[3];
        int listInH  = L[4], ITEM_H = L[5];
        int detailX  = L[6], detailW = L[7];
        int cardY    = L[8], cardH  = L[9];
        int pbx      = L[10], pby  = L[11], pbw = L[12], pbh = L[13];

        Song s   = songs[sel];
        Color ac = hex(s.color);

        // ── Background ────────────────────────────────────────────────────
        g.setPaint(new GradientPaint(0,0,new Color(7,5,20),W,H,new Color(12,7,30)));
        g.fillRect(0,0,W,H);
        float gp = (float)(Math.sin(glow)*0.25+0.75);
        g.setPaint(new RadialGradientPaint((listW+W)/2f, H*1.05f, W*0.5f,
            new float[]{0f,1f}, new Color[]{ca(ac,(int)(50*gp)), new Color(0,0,0,0)}));
        g.fillRect(0,0,W,H);

        // Grid halus
        g.setColor(new Color(255,255,255,4)); g.setStroke(new BasicStroke(0.5f));
        for (int x=0;x<W;x+=50) g.drawLine(x,0,x,H);
        for (int y=0;y<H;y+=50) g.drawLine(0,y,W,y);

        // Partikel
        for (int i=0;i<150;i++) {
            int a = Math.max(0,Math.min(220,(int)(plife[i]*200)));
            g.setColor(new Color(ac.getRed(),ac.getGreen(),ac.getBlue(),a));
            float sz = 2f + plife[i]*5f;
            g.fill(new Ellipse2D.Float(px[i]-sz/2,py[i]-sz/2,sz,sz));
        }

        // ── HEADER ────────────────────────────────────────────────────────
        g.setPaint(new GradientPaint(0,0,new Color(255,255,255,10),0,topH,new Color(0,0,0,0)));
        g.fillRect(0,0,W,topH);
        g.setColor(new Color(255,255,255,15)); g.setStroke(new BasicStroke(1f));
        g.drawLine(0,topH,W,topH);

        int titleSize = Math.max(22,W/40);
        g.setFont(new Font("Arial Black",Font.BOLD,titleSize));
        FontMetrics fm = g.getFontMetrics();
        String titleTxt = "MAGIC TILES";
        int titleX = detailX + (detailW - fm.stringWidth(titleTxt))/2;
        int titleY = (int)(topH*0.65);
        for (int d=4;d>0;d--) { g.setColor(ca(ac,14*d)); g.drawString(titleTxt,titleX,titleY+d); }
        g.setPaint(new GradientPaint(0,titleY-titleSize,new Color(225,200,255),0,titleY,ac));
        g.drawString(titleTxt,titleX,titleY);

        int subSize = Math.max(10,W/100);
        g.setFont(new Font("Arial",Font.PLAIN,subSize)); fm=g.getFontMetrics();
        g.setColor(new Color(175,155,215,155));
        String sub="Indonesia Hits 2026";
        g.drawString(sub, detailX+(detailW-fm.stringWidth(sub))/2, (int)(topH*0.92));

        // ── PANEL KIRI ────────────────────────────────────────────────────
        // Background panel, tinggi = cukup untuk semua item
        g.setColor(new Color(255,255,255,8));
        g.fill(new RoundRectangle2D.Float(listX, listY, listW-listX*2, listInH+4, 14,14));
        g.setColor(new Color(255,255,255,20)); g.setStroke(new BasicStroke(1f));
        g.draw(new RoundRectangle2D.Float(listX, listY, listW-listX*2, listInH+4, 14,14));

        int lblSize = Math.max(12, W/105);
        int lblAreaH = lblSize + 16;
        g.setFont(new Font("Arial", Font.BOLD, lblSize));
        g.setColor(new Color(255,255,255,130));
        g.drawString("PILIH LAGU", listX+12, listY + lblSize + 8);

        // Garis pemisah di bawah label
        g.setColor(new Color(255,255,255,25)); g.setStroke(new BasicStroke(0.8f));
        g.drawLine(listX+8, listY+lblAreaH, listW-listX*2-4, listY+lblAreaH);

        for (int i=0;i<songs.length;i++) {
            int iy = listY + lblAreaH + 4 + i*ITEM_H;
            Song si = songs[i];
            Color sac = hex(si.color);
            boolean isSel = (i==sel);

            int ix = listX+4, iw = listW-listX*2-8;

            if (isSel) {
                g.setPaint(new GradientPaint(ix,iy,ca(sac,70),ix+iw,iy,ca(sac,20)));
                g.fill(new RoundRectangle2D.Float(ix,iy,iw,ITEM_H-4,10,10));
                g.setColor(ca(sac,170)); g.setStroke(new BasicStroke(1.5f));
                g.draw(new RoundRectangle2D.Float(ix,iy,iw,ITEM_H-4,10,10));
            } else if (i>0) {
                g.setColor(new Color(255,255,255,10)); g.setStroke(new BasicStroke(0.5f));
                g.drawLine(ix+4,iy,ix+iw-4,iy);
            }

            // Dot
            int dotX = ix+10, dotY2 = iy+(ITEM_H-4)/2-4;
            if (isSel) { g.setColor(ca(sac,55)); g.fill(new Ellipse2D.Float(dotX-3,dotY2-3,14,14)); }
            g.setColor(sac); g.fill(new Ellipse2D.Float(dotX,dotY2,8,8));

            int maxTW = iw-30;
            int tSize = Math.max(13, W/95);
            g.setFont(new Font("Arial",isSel?Font.BOLD:Font.PLAIN,tSize));
            g.setColor(isSel?Color.WHITE:new Color(195,195,218));
            fm = g.getFontMetrics();
            String t = si.title;
            while (fm.stringWidth(t)>maxTW && t.length()>8) t=t.substring(0,t.length()-1);
            if (!t.equals(si.title)) t=t.trim()+"...";
            g.drawString(t, ix+24, iy+tSize+8);

            int aSize = Math.max(11, W/115);
            g.setFont(new Font("Arial",Font.PLAIN,aSize));
            g.setColor(isSel?ca(sac,200):new Color(140,140,165));
            fm = g.getFontMetrics();
            String ar = si.artist;
            while (fm.stringWidth(ar)>maxTW && ar.length()>8) ar=ar.substring(0,ar.length()-1);
            if (!ar.equals(si.artist)) ar=ar.trim()+"...";
            g.drawString(ar, ix+24, iy+tSize+aSize+12);

            int tagS = Math.max(10, W/128);
            g.setFont(new Font("Consolas",Font.PLAIN,tagS));
            g.setColor(isSel?ca(sac,160):new Color(100,100,125));
            g.drawString(si.bpmLabel+"  "+si.diffLabel, ix+24, iy+ITEM_H-10);
        }

        // ── PANEL KANAN: Detail card ──────────────────────────────────────
        int cardR=18;
        for (int sh=6;sh>0;sh--) { g.setColor(new Color(0,0,0,9*sh)); g.fill(new RoundRectangle2D.Float(detailX+sh,cardY+sh,detailW-sh,cardH,cardR,cardR)); }
        g.setPaint(new GradientPaint(detailX,cardY,new Color(255,255,255,13),detailX,cardY+cardH,ca(ac,7)));
        g.fill(new RoundRectangle2D.Float(detailX,cardY,detailW,cardH,cardR,cardR));
        float ba=(float)(Math.sin(glow)*0.15+0.55);
        g.setColor(ca(ac,(int)(ba*185))); g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Float(detailX,cardY,detailW,cardH,cardR,cardR));
        g.setColor(new Color(255,255,255,18)); g.setStroke(new BasicStroke(1f));
        g.drawLine(detailX+cardR,cardY+1,detailX+detailW-cardR,cardY+1);

        // Album art circle — ukuran & posisi absolut, centered vertikal di card
        int artSize = Math.min((int)(detailW*0.28), (int)(H*0.28));
        int contentH = artSize + 20 + 24 + 20 + 28 + 16; // art + gap + title + artist + gap + badge
        int contentStartY = cardY + (cardH - contentH) / 2;
        int artX = detailX + (detailW - artSize) / 2;
        int artY = contentStartY;
        float pulse = (float)(1.0 + 0.05 * Math.sin(glow * 1.6));
        int rEx=(int)(artSize*0.09*pulse);
        g.setColor(ca(ac,18)); g.fill(new Ellipse2D.Float(artX-rEx,artY-rEx,artSize+rEx*2,artSize+rEx*2));
        g.setPaint(new GradientPaint(artX,artY,ca(ac,60),artX+artSize,artY+artSize,new Color(0,0,0,110)));
        g.fill(new Ellipse2D.Float(artX,artY,artSize,artSize));
        g.setColor(ca(ac,150)); g.setStroke(new BasicStroke(2f));
        g.draw(new Ellipse2D.Float(artX,artY,artSize,artSize));

        // Load cover sekali saja saat lagu berubah
        if (cachedSel != sel) {
            cachedSel = sel;
            cachedCover = null;
            try {
                // Coba PNG dulu, kalau ga ada coba JPG
                java.io.File f = new java.io.File("cover/" + (sel+1) + ".png");
                if (!f.exists()) f = new java.io.File("cover/" + (sel+1) + ".jpg");
                if (f.exists()) cachedCover = javax.imageio.ImageIO.read(f);
            } catch (Exception e) { System.out.println("Error: " + e); }
        }
        if (cachedCover != null) {
            Shape oldClip = g.getClip();
            g.setClip(new Ellipse2D.Float(artX, artY, artSize, artSize));
            g.drawImage(cachedCover, artX, artY, artSize, artSize, null);
            g.setClip(oldClip);
            g.setColor(new Color(0,0,0,30));
            g.fill(new Ellipse2D.Float(artX, artY, artSize, artSize));
        } else {
            drawMusicNote(g, detailX+detailW/2, artY+artSize/2, (int)(artSize*0.38), glow);
        }

        // Info lagu
        int infoY = artY + artSize + 18;
        int stSize = Math.max(15,W/55);
        g.setFont(new Font("Arial Black",Font.BOLD,stSize)); fm=g.getFontMetrics();
        g.setColor(Color.WHITE);
        String dt=s.title; while(fm.stringWidth(dt)>detailW-24&&dt.length()>4) dt=dt.substring(0,dt.length()-4)+"...";
        g.drawString(dt, detailX+(detailW-fm.stringWidth(dt))/2, infoY+stSize);

        int arSize2=Math.max(11,W/85);
        g.setFont(new Font("Arial",Font.PLAIN,arSize2)); fm=g.getFontMetrics();
        g.setColor(ca(ac,215));
        g.drawString(s.artist, detailX+(detailW-fm.stringWidth(s.artist))/2, infoY+stSize+arSize2+7);

        // Badge BPM + Difficulty
        int badgeY2 = infoY + stSize + arSize2 + 20;
        int bSize=Math.max(10,W/108), bPad=14;
        g.setFont(new Font("Consolas",Font.BOLD,bSize)); fm=g.getFontMetrics();
        String bpmStr=s.bpm+" BPM";
        int bw=fm.stringWidth(bpmStr)+bPad*2, bh2=bSize+12;
        g.setFont(new Font("Arial Black",Font.BOLD,bSize)); FontMetrics fm2=g.getFontMetrics();
        String diffStr=s.diffLabel;
        int dw=fm2.stringWidth(diffStr)+bPad*2;
        int badgeStartX=detailX+(detailW-bw-10-dw)/2;

        g.setColor(ca(ac,38)); g.fill(new RoundRectangle2D.Float(badgeStartX,badgeY2,bw,bh2,bh2,bh2));
        g.setColor(ca(ac,130)); g.setStroke(new BasicStroke(1f)); g.draw(new RoundRectangle2D.Float(badgeStartX,badgeY2,bw,bh2,bh2,bh2));
        g.setFont(new Font("Consolas",Font.BOLD,bSize)); g.setColor(ac);
        g.drawString(bpmStr, badgeStartX+bPad, badgeY2+bSize+4);

        Color dc=diffStr.equals("MUDAH")?new Color(65,215,110):diffStr.equals("SULIT")?new Color(255,70,70):new Color(255,190,50);
        int diffBX=badgeStartX+bw+10;
        g.setColor(new Color(dc.getRed(),dc.getGreen(),dc.getBlue(),38));
        g.fill(new RoundRectangle2D.Float(diffBX,badgeY2,dw,bh2,bh2,bh2));
        g.setColor(new Color(dc.getRed(),dc.getGreen(),dc.getBlue(),170)); g.setStroke(new BasicStroke(1f));
        g.draw(new RoundRectangle2D.Float(diffBX,badgeY2,dw,bh2,bh2,bh2));
        g.setFont(new Font("Arial Black",Font.BOLD,bSize)); g.setColor(dc);
        g.drawString(diffStr, diffBX+bPad, badgeY2+bSize+4);

        // ── PLAY BUTTON ───────────────────────────────────────────────────
        float pp=(float)(Math.sin(glow*1.4)*0.1+0.9);
        for (int i=5;i>0;i--) { g.setColor(ca(ac,(int)(16*pp*i/5))); g.fill(new RoundRectangle2D.Float(pbx-i*3,pby-i*2,pbw+i*6,pbh+i*4,40,40)); }
        g.setPaint(new GradientPaint(pbx,pby,lighter(ac,55),pbx,pby+pbh,ac));
        g.fill(new RoundRectangle2D.Float(pbx,pby,pbw,pbh,32,32));
        g.setColor(new Color(255,255,255,32)); g.fill(new RoundRectangle2D.Float(pbx+5,pby+4,pbw-10,pbh/2-4,22,22));
        g.setColor(new Color(255,255,255,45)); g.setStroke(new BasicStroke(1f)); g.draw(new RoundRectangle2D.Float(pbx,pby,pbw,pbh,32,32));

        int playSize=Math.max(13,W/72);
        g.setFont(new Font("Arial Black",Font.BOLD,playSize)); fm=g.getFontMetrics();
        String play="MAIN SEKARANG";
        int playX=pbx+(pbw-fm.stringWidth(play))/2;
        int playY2=pby+pbh/2+playSize/2-1;
        g.setColor(new Color(0,0,0,55)); g.drawString(play,playX+1,playY2+1);
        g.setColor(Color.WHITE); g.drawString(play,playX,playY2);

        // Dots
        int dotAreaY=pby+pbh+10, dotGap=16;
        int dotSX=detailX+(detailW-songs.length*dotGap)/2;
        for (int i=0;i<songs.length;i++) {
            int dx=dotSX+i*dotGap;
            if (i==sel) { g.setColor(ac); g.fill(new RoundRectangle2D.Float(dx-7,dotAreaY,14,7,7,7)); }
            else { g.setColor(new Color(255,255,255,40)); g.fill(new Ellipse2D.Float(dx-3,dotAreaY+1,6,6)); }
        }

        // Hint
        int hintSize=Math.max(9,W/138);
        g.setFont(new Font("Arial",Font.PLAIN,hintSize)); fm=g.getFontMetrics();
        g.setColor(new Color(255,255,255,42));
        String hint="Panah = ganti lagu   |   ASDF/KL = ketuk   |   ENTER = main   |   ESC = keluar";
        g.drawString(hint, W/2-fm.stringWidth(hint)/2, H-6);
    }

    private void drawMusicNote(Graphics2D g, int cx, int cy, int size, float glow) {
        AffineTransform old = g.getTransform();
        float sc=(float)(1.0+0.04*Math.sin(glow*2));
        g.translate(cx,cy); g.scale(sc,sc);
        g.setStroke(new BasicStroke(size*0.13f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        // Stem
        g.setColor(new Color(255,255,255,200));
        g.drawLine(size/5, -size/2, size/5, size/4);
        // Flag
        g.drawLine(size/5, -size/2, size*2/3, -size/5);
        // Note head
        AffineTransform t2=g.getTransform(); g.rotate(-0.3);
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Float(-size/4, size/5, size*2/5, size/4));
        g.setTransform(t2);
        g.setTransform(old);
    }

    static Color hex(String h) { return Color.decode(h); }
    static Color ca(Color c,int a) { a=Math.max(0,Math.min(255,a)); return new Color(c.getRed(),c.getGreen(),c.getBlue(),a); }
    static Color lighter(Color c,int amt) { return new Color(Math.min(255,c.getRed()+amt),Math.min(255,c.getGreen()+amt),Math.min(255,c.getBlue()+amt)); }
}
