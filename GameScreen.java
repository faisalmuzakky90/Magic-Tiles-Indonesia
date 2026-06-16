import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

public class GameScreen extends JPanel {
    static final int COLS = 4;
    static final int GAME_W = 860;

    int W, H, GAME_X, CW, TH, HIT_Y, HIT_H;

    private JFrame frame;
    private Song song;
    private int songIdx;
    private Color ac;
    private MusicEngine music;

    private ArrayList<float[]> tiles   = new ArrayList<>();
    private ArrayList<float[]> hitFx   = new ArrayList<>();
    private ArrayList<Object[]> scoreFx = new ArrayList<>();
    private ArrayList<float[]> parts   = new ArrayList<>();

    private int score=0, combo=0, maxCombo=0;
    private int perfect=0, good=0, okCount=0, miss=0;
    private float[] colFlash = new float[4];
    private boolean[] keyHeld = new boolean[4];
    private float missFlash=0, glow=0, speed=4;
    private int patIdx=0;
    private long lastSpawn=0, gameStart=0;
    private boolean gameOver=false, paused=false, gameWin=false;
    private int tilesTotal=0, tilesHit=0;
    private long audioPositionOnDeath=0;
    private Random rand = new Random();
    private int countdown=0;
    private float countdownAlpha=0;
    private volatile boolean cancelled=false;

    public GameScreen(JFrame frame, Song song, int songIdx) {
        this.frame=frame; this.song=song; this.songIdx=songIdx;
        this.W=MagicTilesGame.W; this.H=MagicTilesGame.H;
        this.GAME_X=(W-GAME_W)/2 - 40;
        this.CW=GAME_W/COLS;
        this.TH=110;
        this.HIT_H=H/7;
        this.HIT_Y=H-HIT_H-H/5;
        this.ac=MenuScreen.hex(song.color);

        setPreferredSize(new Dimension(W,H));
        setBackground(Color.BLACK);
        setFocusable(true);

        music=new MusicEngine(); // lagu dimulai setelah countdown

        for(int i=0;i<8;i++){
            int col=song.pattern[patIdx%song.pattern.length];
            tiles.add(new float[]{col,-TH*(i+1)-i*8f,0});
            patIdx++;
        }

        setupKeys();
        addMouseListener(new MouseAdapter(){
            public void mousePressed(MouseEvent e){
                if(gameOver){handleGoClick(e.getX(),e.getY());return;}
                if(gameWin){handleWinClick(e.getX(),e.getY());return;}
                if(paused) return;
                int col=(e.getX()-GAME_X)/CW;
                if(col>=0&&col<COLS) hit(col);
            }
        });

        gameStart=System.currentTimeMillis();
        new Timer(16,e->loop()).start();
        startCountdown(false);
    }

    void startCountdown(boolean isResume){
        paused=true; countdown=3; countdownAlpha=1f;
        new Thread(()->{
            try{
                for(int i=3;i>0;i--){
                    if(cancelled) return; // stop kalau udah di-cancel
                    final int c=i;
                    SwingUtilities.invokeLater(()->{countdown=c;countdownAlpha=1f;repaint();});
                    Thread.sleep(1000);
                }
                if(cancelled) return; // stop sebelum mulai musik
                SwingUtilities.invokeLater(()->{
                    countdown=0; paused=false;
                    if(isResume){
                        music.setSongResume(songIdx,song.title,audioPositionOnDeath);
                        audioPositionOnDeath=0;
                    } else {
                        music.setSong(songIdx,song.title); // mulai lagu setelah 3 2 1
                    }
                });
            }catch(Exception ignored){}
        }).start();
    }

    void setupKeys(){
        InputMap im=getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am=getActionMap();
        int[][] keyMap={
            {KeyEvent.VK_A,0},{KeyEvent.VK_S,1},{KeyEvent.VK_D,2},{KeyEvent.VK_F,3},
            {KeyEvent.VK_K,2},{KeyEvent.VK_L,3},
        };
        for(int[] km:keyMap){
            final int key=km[0],col=km[1];
            im.put(KeyStroke.getKeyStroke(key,0,false),"p"+key);
            am.put("p"+key,new AbstractAction(){public void actionPerformed(ActionEvent e){
                if(!keyHeld[col]){keyHeld[col]=true;if(!gameOver&&!paused)hit(col);}}});
            im.put(KeyStroke.getKeyStroke(key,0,true),"r"+key);
            am.put("r"+key,new AbstractAction(){public void actionPerformed(ActionEvent e){keyHeld[col]=false;}});
        }
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),"esc");
        am.put("esc",new AbstractAction(){public void actionPerformed(ActionEvent e){goMenu();}});
    }

    void loop(){
        if(paused||gameOver||gameWin){
            if(countdown>0) countdownAlpha=Math.max(0,countdownAlpha-0.015f);
            repaint(); return;
        }
        glow+=0.05f;
        long now=System.currentTimeMillis();
        long elapsed=now-gameStart;
        speed=(float)Math.min(9,4+elapsed/15000.0*1.5);
        int interval=(int)(60000.0/song.bpm*0.82*(4.0/speed));

        // Cek lagu selesai (via audio)
        if(music.isAudioFinished()&&!gameWin){
            gameWin=true; music.close();
        }

        if(now-lastSpawn>interval){
            int col=song.pattern[patIdx%song.pattern.length];
            float topY=-TH;
            for(float[] t:tiles) if(t[2]==0&&t[1]<topY) topY=t[1];
            tiles.add(new float[]{col,Math.min(topY-TH-8,-TH),0});
            patIdx++; lastSpawn=now;
        }

        for(int i=tiles.size()-1;i>=0;i--){
            float[] t=tiles.get(i);
            t[1]+=speed;
            if(t[2]==0&&t[1]>H+10){
                tiles.remove(i); combo=0; missFlash=1; miss++;
                music.playMissSound();
                if(miss>=8){gameOver=true;audioPositionOnDeath=music.getAudioPosition();music.pauseAudio();}
            }else if(t[2]==1&&t[1]>H+20) tiles.remove(i);
        }

        for(int c=0;c<4;c++) if(colFlash[c]>0) colFlash[c]-=0.08f;
        if(missFlash>0) missFlash-=0.045f;
        hitFx.forEach(f->{f[2]+=3;f[3]-=0.06f;}); hitFx.removeIf(f->f[3]<=0);
        scoreFx.forEach(f->{f[1]=(float)f[1]-1.4f;f[4]=(float)f[4]-0.022f;}); scoreFx.removeIf(f->(float)f[4]<=0);
        parts.forEach(p->{p[0]+=p[2];p[1]+=p[3];p[3]+=0.14f;p[4]-=p[5];}); parts.removeIf(p->p[4]<=0);
        repaint();
    }

    void hit(int col){
        colFlash[col]=1;
        float[] best=null; float bestDist=Float.MAX_VALUE;
        for(float[] t:tiles){
            if((int)t[0]==col&&t[2]==0){
                float d=Math.abs((t[1]+TH/2f)-(HIT_Y+HIT_H/2f));
                if(d<150&&d<bestDist){bestDist=d;best=t;}
            }
        }
        if(best!=null){
            best[2]=1; combo++; if(combo>maxCombo) maxCombo=combo; tilesHit++;
            int pts; String lbl; Color fc;
            if(bestDist<30){pts=100+combo*2;lbl="PERFECT!";fc=new Color(255,215,0);perfect++;music.playPerfectSound();}
            else if(bestDist<75){pts=50+combo*1;lbl="GOOD";fc=ac;good++;music.playHitNote(col);}
            else{pts=20;lbl="OK";fc=new Color(100,220,100);okCount++;music.playHitNote(col);}
            score+=pts;
            hitFx.add(new float[]{GAME_X+col*CW+CW/2f,best[1]+TH/2f,15,1});
            scoreFx.add(new Object[]{(float)(GAME_X+col*CW+6),(float)(HIT_Y-35),"+"+pts+" "+lbl,fc,1f});
            spawnParts((int)(GAME_X+col*CW+CW/2),(int)(best[1]+TH/2));
        }else{
            combo=0; miss++;
            scoreFx.add(new Object[]{(float)(GAME_X+col*CW+6),(float)(HIT_Y-35),"MISS",new Color(255,70,70),1f});
            if(miss>=8){gameOver=true;audioPositionOnDeath=music.getAudioPosition();music.pauseAudio();}
        }
    }

    void spawnParts(int x,int y){
        for(int i=0;i<14;i++){
            float a=(float)(Math.random()*Math.PI*2),spd=2+rand.nextFloat()*5;
            float decay=0.035f+rand.nextFloat()*0.03f;
            parts.add(new float[]{x,y,(float)(Math.cos(a)*spd),(float)(Math.sin(a)*spd),1,decay});
        }
    }

    void goMenu(){
        cancelled=true; // cancel countdown thread
        music.close();
        MenuScreen m=new MenuScreen(frame);
        frame.getContentPane().removeAll(); frame.getContentPane().add(m);
        frame.setSize(W,H); frame.revalidate(); m.requestFocusInWindow();
    }

    void handleGoClick(int mx,int my){
        int px=W/2-200,py=H/2-220,pw=400;
        int bw=(pw-42)/3,by2=py+360,bh=50;
        if(mx>px+12&&mx<px+12+bw&&my>by2&&my<by2+bh){goMenu();return;}
        if(mx>px+15+bw&&mx<px+15+bw*2&&my>by2&&my<by2+bh){resumeGame();return;}
        if(mx>px+18+bw*2&&mx<px+pw-12&&my>by2&&my<by2+bh){
            music.close();
            GameScreen ng=new GameScreen(frame,song,songIdx);
            frame.getContentPane().removeAll(); frame.getContentPane().add(ng);
            frame.setSize(W,H); frame.revalidate(); ng.requestFocusInWindow();
        }
    }

    void handleWinClick(int mx, int my){
        int px=W/2-200, py=H/2-195, pw=400;
        int bw=(pw-42)/3, by2=py+348, bh=50;
        int b1x=px+12,       b1w=bw;
        int b2x=px+15+bw,    b2w=bw;
        int b3x=px+18+bw*2,  b3w=pw-18-bw*2-12;

        if(my>=by2&&my<=by2+bh){
            if(mx>=b1x&&mx<=b1x+b1w){ goMenu(); return; }
            if(mx>=b2x&&mx<=b2x+b2w){
                music.close();
                GameScreen ng=new GameScreen(frame,song,songIdx);
                frame.getContentPane().removeAll(); frame.getContentPane().add(ng);
                frame.setSize(W,H); frame.revalidate(); ng.requestFocusInWindow();
                return;
            }
            if(mx>=b3x&&mx<=b3x+b3w){
                Song[] allSongs=Song.getSongs();
                int nextIdx=(songIdx+1)%allSongs.length;
                music.close();
                GameScreen ng=new GameScreen(frame,allSongs[nextIdx],nextIdx);
                frame.getContentPane().removeAll(); frame.getContentPane().add(ng);
                frame.setSize(W,H); frame.revalidate(); ng.requestFocusInWindow();
                return;
            }
        }
    }

    void resumeGame(){
        miss=Math.max(0,miss-5);
        gameOver=false;
        startCountdown(true);
    }

    @Override
    protected void paintComponent(Graphics g0){
        super.paintComponent(g0);
        Graphics2D g=(Graphics2D)g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // BG
        g.setColor(new Color(5,5,18)); g.fillRect(0,0,W,H);
        if(missFlash>0){g.setColor(new Color(255,0,0,Math.max(0,Math.min(255,(int)(missFlash*70)))));g.fillRect(0,0,W,H);}

        // Area gelap kiri kanan
        g.setColor(new Color(0,0,0,160));
        g.fillRect(0,0,GAME_X,H);
        g.fillRect(GAME_X+GAME_W,0,W-GAME_X-GAME_W,H);

        // Border area game
        g.setColor(MenuScreen.ca(ac,65)); g.setStroke(new BasicStroke(1.2f));
        g.drawLine(GAME_X,0,GAME_X,H);
        g.drawLine(GAME_X+GAME_W,0,GAME_X+GAME_W,H);

        // Lanes
        for(int c=0;c<COLS;c++){
            int lx=GAME_X+c*CW;
            if(colFlash[c]>0){g.setColor(MenuScreen.ca(ac,Math.max(0,Math.min(255,(int)(colFlash[c]*55)))));g.fillRect(lx,0,CW,H);}
            g.setColor(MenuScreen.ca(ac,65)); g.setStroke(new BasicStroke(1.1f));
            if(c>0) g.drawLine(lx,0,lx,H);
            g.setStroke(new BasicStroke(1f));
        }

        // Hit zone
        GradientPaint hz=new GradientPaint(0,HIT_Y,MenuScreen.ca(ac,55),0,HIT_Y+HIT_H,new Color(0,0,0,30));
        g.setPaint(hz); g.fillRect(GAME_X,HIT_Y,GAME_W,HIT_H);
        float lp=(float)(Math.sin(glow)*0.4+0.6);
        for(int t=5;t>0;t--){g.setColor(MenuScreen.ca(ac,Math.max(0,Math.min(255,(int)(lp*60/t)))));g.setStroke(new BasicStroke(t*1.8f));g.drawLine(GAME_X,HIT_Y,GAME_X+GAME_W,HIT_Y);}
        g.setColor(MenuScreen.ca(ac,Math.max(0,Math.min(255,(int)(lp*220))))); g.setStroke(new BasicStroke(2f)); g.drawLine(GAME_X,HIT_Y,GAME_X+GAME_W,HIT_Y);

        // Key labels
        String[] KEYS={"A","S","D","F"};
        String[] KEYS2={"","","K","L"};
        for(int c=0;c<COLS;c++){
            int cx2=GAME_X+c*CW+CW/2;
            float a=(float)(0.3+colFlash[c]*0.7);
            g.setFont(new Font("Arial Black",Font.BOLD,22));
            g.setColor(MenuScreen.ca(ac,Math.max(0,Math.min(255,(int)(a*200)))));
            FontMetrics fm=g.getFontMetrics();
            g.drawString(KEYS[c],cx2-fm.stringWidth(KEYS[c])/2,H-22);
            if(!KEYS2[c].isEmpty()){
                g.setFont(new Font("Arial",Font.PLAIN,12));
                g.setColor(MenuScreen.ca(ac,Math.max(0,Math.min(255,(int)(a*120)))));
                fm=g.getFontMetrics();
                g.drawString(KEYS2[c],cx2-fm.stringWidth(KEYS2[c])/2,H-6);
            }
        }

        // Tiles
        for(float[] t:tiles){
            if(t[2]==1) continue;
            int tx=GAME_X+(int)t[0]*CW+6, ty=(int)t[1];
            int tw=CW-12, th=TH-8;
            float dist=Math.abs((t[1]+TH/2f)-(HIT_Y+HIT_H/2f));
            Color top,bot;
            if(dist<120){top=MenuScreen.lighter(ac,55);bot=ac;}
            else{top=new Color(225,225,248);bot=new Color(175,175,210);}
            g.setPaint(new GradientPaint(tx,ty,top,tx,ty+th,bot));
            g.fill(new RoundRectangle2D.Float(tx,ty,tw,th,12,12));
            g.setColor(new Color(255,255,255,55));
            g.fill(new RoundRectangle2D.Float(tx+3,ty+3,tw-6,th/3,8,8));
            if(dist<120){
                float ga=(120-dist)/120f;
                g.setColor(MenuScreen.ca(ac,Math.max(0,Math.min(255,(int)(ga*180)))));
                g.setStroke(new BasicStroke(2f));
                g.draw(new RoundRectangle2D.Float(tx,ty,tw,th,12,12));
            }
        }

        // Hit FX
        for(float[] f:hitFx){
            g.setColor(MenuScreen.ca(ac,Math.max(0,Math.min(255,(int)(f[3]*160)))));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Ellipse2D.Float(f[0]-f[2],f[1]-f[2],f[2]*2,f[2]*2));
            g.setColor(MenuScreen.ca(ac,Math.max(0,Math.min(255,(int)(f[3]*50)))));
            g.fill(new Ellipse2D.Float(f[0]-f[2]*0.6f,f[1]-f[2]*0.6f,f[2]*1.2f,f[2]*1.2f));
        }

        // Score floats
        for(Object[] sf:scoreFx){
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,Math.max(0,Math.min(1,(float)sf[4]))));
            g.setFont(new Font("Arial Black",Font.BOLD,15));
            g.setColor(new Color(0,0,0,100));
            g.drawString((String)sf[2],(int)(float)(Float)sf[0]+1,(int)(float)(Float)sf[1]+1);
            g.setColor((Color)sf[3]);
            g.drawString((String)sf[2],(int)(float)(Float)sf[0],(int)(float)(Float)sf[1]);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1f));
        }

        // Particles
        for(float[] p:parts){
            g.setColor(MenuScreen.ca(ac,Math.max(0,Math.min(255,(int)(p[4]*180)))));
            g.fill(new Ellipse2D.Float(p[0]-p[5]*60,p[1]-p[5]*60,p[5]*120,p[5]*120));
        }

        // HUD
        g.setColor(new Color(0,0,0,180)); g.fillRect(0,0,W,80);
        g.setColor(new Color(255,255,255,15)); g.setStroke(new BasicStroke(1f)); g.drawLine(0,80,W,80);
        g.setFont(new Font("Arial Black",Font.BOLD,14)); g.setColor(ac);
        g.drawString(song.title.length()>30?song.title.substring(0,28)+"...":song.title,16,24);
        g.setFont(new Font("Arial",Font.PLAIN,11)); g.setColor(new Color(255,255,255,150));
        g.drawString(song.artist,16,40);

        // Score
        g.setFont(new Font("Consolas",Font.BOLD,18));
        FontMetrics fm=g.getFontMetrics();
        String sc=String.format("%06d",score);
        g.setColor(MenuScreen.ca(ac,70)); g.drawString(sc,GAME_X+GAME_W-fm.stringWidth(sc)-10,36);
        g.setColor(Color.WHITE); g.drawString(sc,GAME_X+GAME_W-fm.stringWidth(sc)-11,35);

        // Combo
        if(combo>1){
            g.setFont(new Font("Arial Black",Font.BOLD,12)); fm=g.getFontMetrics();
            String cs="x"+combo+" COMBO";
            g.setColor(MenuScreen.ca(ac,200)); g.drawString(cs,GAME_X+GAME_W-fm.stringWidth(cs)-11,54);
        }

        // Hearts
        for(int i=0;i<8;i++){
            g.setFont(new Font("Arial",Font.BOLD,13));
            g.setColor(i<(8-miss)?new Color(255,80,100):new Color(70,70,90));
            g.drawString(i<(8-miss)?"<3":"[x]",16+i*22,62);
        }

        // Speed
        g.setFont(new Font("Consolas",Font.PLAIN,11)); g.setColor(new Color(255,255,255,70));
        g.drawString(String.format("%.1fx",speed/4f),GAME_X+GAME_W-50,62);

        // Hint kontrol — strip kecil tepat di bawah HUD utama
        if(!gameOver&&!gameWin){
            int PW = getWidth() > 0 ? getWidth() : W;
            g.setColor(new Color(0,0,0,120));
            g.fillRect(0,80,PW,22);
            g.setColor(new Color(255,255,255,15)); g.setStroke(new BasicStroke(1f));
            g.drawLine(0,102,PW,102);
            g.setFont(new Font("Arial",Font.PLAIN,12));
            FontMetrics fmHint=g.getFontMetrics();
            String hint="Tekan A S D F atau A S K L untuk memukul tiles";
            g.setColor(new Color(255,255,255,120));
            g.drawString(hint,(PW-fmHint.stringWidth(hint))/2,96);
        }

        // Countdown
        if(paused&&countdown>0) drawCountdown(g);
        if(gameWin) drawWin(g);
        else if(gameOver) drawGameOver(g);
    }

    void drawCountdown(Graphics2D g){
        g.setColor(new Color(0,0,0,120)); g.fillRect(0,0,W,H);
        String num=String.valueOf(countdown);
        int size=180;
        g.setFont(new Font("Arial Black",Font.BOLD,size));
        FontMetrics fm=g.getFontMetrics();
        int tx=GAME_X+(GAME_W-fm.stringWidth(num))/2;
        int ty=H/2+size/3;
        g.setColor(MenuScreen.ca(ac,Math.max(0,Math.min(255,(int)(countdownAlpha*80)))));
        for(int d=6;d>0;d-=2) g.drawString(num,tx+d,ty+d);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,Math.max(0,Math.min(1,countdownAlpha))));
        GradientPaint gp=new GradientPaint(tx,ty-size,Color.WHITE,tx,ty,ac);
        g.setPaint(gp); g.drawString(num,tx,ty);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1f));
        g.setFont(new Font("Arial Black",Font.BOLD,24)); fm=g.getFontMetrics();
        g.setColor(MenuScreen.ca(ac,Math.max(0,Math.min(255,(int)(countdownAlpha*180)))));
        String siap="SIAP-SIAP...";
        g.drawString(siap,GAME_X+(GAME_W-fm.stringWidth(siap))/2,H/2+size/3+50);
    }


    void drawWin(Graphics2D g){
        // Overlay
        g.setColor(new Color(0,5,0,185)); g.fillRect(0,0,W,H);
        int px=W/2-200,py=H/2-195,pw=400,ph=410;
        g.setColor(new Color(8,35,15,245));
        g.fill(new RoundRectangle2D.Float(px,py,pw,ph,28,28));
        // Border glow
        g.setColor(new Color(60,220,100)); g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Float(px,py,pw,ph,28,28));

        // Judul
        g.setFont(new Font("Arial Black",Font.BOLD,38));
        FontMetrics fm=g.getFontMetrics(); String title="SELESAI!";
        g.setColor(new Color(0,255,100,80));
        for(int d=2;d>0;d--) g.drawString(title,px+(pw-fm.stringWidth(title))/2+d,py+55+d);
        GradientPaint gp=new GradientPaint(0,py,new Color(180,255,200),0,py+55,new Color(60,220,100));
        g.setPaint(gp); g.drawString(title,px+(pw-fm.stringWidth(title))/2,py+55);

        // Skor
        g.setFont(new Font("Consolas",Font.BOLD,44)); fm=g.getFontMetrics();
        String sc=String.format("%06d",score);
        g.setColor(Color.WHITE); g.drawString(sc,(W-fm.stringWidth(sc))/2,py+108);

        // Bintang
        int stars=score>8000?3:score>3000?2:score>500?1:0;
        g.setFont(new Font("Arial Black",Font.BOLD,42)); fm=g.getFontMetrics();
        String starStr=stars==3?"* * *":stars==2?"* *":stars==1?"*":"";
        g.setColor(new Color(255,215,0));
        g.drawString(starStr,(W-fm.stringWidth(starStr))/2,py+155);
        g.setFont(new Font("Arial Black",Font.BOLD,16)); fm=g.getFontMetrics();
        String rank=stars==3?"SEMPURNA!":stars==2?"BAGUS BANGET!":stars==1?"LUMAYAN!":"KEEP TRYING!";
        g.setColor(stars==3?new Color(255,215,0):new Color(60,220,100));
        g.drawString(rank,(W-fm.stringWidth(rank))/2,py+178);

        // Stats
        String[] labels={"PERFECT","GOOD","OK","MISS"};
        int[] vals={perfect,good,okCount,miss};
        Color[] cols={new Color(255,215,0),new Color(60,220,100),new Color(100,220,100),new Color(255,80,80)};
        for(int i=0;i<4;i++){
            int sy=py+188+i*38;
            g.setColor(new Color(255,255,255,15));
            g.fill(new RoundRectangle2D.Float(px+16,sy,pw-32,32,8,8));
            g.setFont(new Font("Arial",Font.PLAIN,12)); g.setColor(new Color(255,255,255,120));
            g.drawString(labels[i],px+28,sy+21);
            g.setFont(new Font("Arial Black",Font.BOLD,15)); g.setColor(cols[i]);
            String v=String.valueOf(vals[i]);
            fm=g.getFontMetrics(); g.drawString(v,px+pw-28-fm.stringWidth(v),sy+21);
        }

        // Buttons
        int bw3=(pw-42)/3,by3=py+348,bh3=50;
        drawBtn(g,px+12,by3,bw3,bh3,"MENU",new Color(70,70,120),new Color(130,130,200));
        drawBtn(g,px+15+bw3,by3,bw3,bh3,"ULANG",new Color(100,60,0),new Color(255,180,50));
        drawBtn(g,px+18+bw3*2,by3,bw3,bh3,"LANJUT >>",new Color(30,100,60),new Color(60,220,100));

        g.setFont(new Font("Arial",Font.PLAIN,10)); g.setColor(new Color(60,220,100,150));
        fm=g.getFontMetrics(); String next=">> lagu berikutnya";
        g.drawString(next,px+18+bw3*2+(bw3-fm.stringWidth(next))/2,by3+bh3+15);
    }

    void drawGameOver(Graphics2D g){
        g.setColor(new Color(0,0,5,185)); g.fillRect(0,0,W,H);
        int px=W/2-200,py=H/2-220,pw=400,ph=430;
        g.setColor(new Color(12,8,35,240));
        g.fill(new RoundRectangle2D.Float(px,py,pw,ph,28,28));
        g.setColor(ac); g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Float(px,py,pw,ph,28,28));

        g.setFont(new Font("Arial Black",Font.BOLD,36));
        FontMetrics fm=g.getFontMetrics(); String go="GAME OVER";
        g.setColor(MenuScreen.ca(ac,80));
        for(int d=2;d>0;d--) g.drawString(go,px+(pw-fm.stringWidth(go))/2+d,py+58+d);
        g.setColor(Color.WHITE); g.drawString(go,px+(pw-fm.stringWidth(go))/2,py+58);

        g.setFont(new Font("Consolas",Font.BOLD,44)); fm=g.getFontMetrics();
        String sc=String.format("%06d",score);
        g.setColor(Color.WHITE); g.drawString(sc,(W-fm.stringWidth(sc))/2,py+112);

        int stars=score>5000?3:score>2000?2:score>300?1:0;
        g.setFont(new Font("Arial Black",Font.BOLD,22)); fm=g.getFontMetrics();
        String starStr=stars==3?"*** SEMPURNA ***":stars==2?"** BAGUS **":stars==1?"* OKE *":"-- coba lagi --";
        g.setColor(stars==3?new Color(255,215,0):stars==2?ac:new Color(180,180,180));
        g.drawString(starStr,(W-fm.stringWidth(starStr))/2,py+148);

        String[] labels={"PERFECT","GOOD","OK","MISS"};
        int[] vals={perfect,good,okCount,miss};
        Color[] cols={new Color(255,215,0),ac,new Color(100,220,100),new Color(255,80,80)};
        for(int i=0;i<4;i++){
            int sy=py+165+i*42;
            g.setColor(new Color(255,255,255,18));
            g.fill(new RoundRectangle2D.Float(px+16,sy,pw-32,36,8,8));
            g.setFont(new Font("Arial",Font.PLAIN,13)); g.setColor(new Color(255,255,255,130));
            g.drawString(labels[i],px+28,sy+24);
            g.setFont(new Font("Arial Black",Font.BOLD,17)); g.setColor(cols[i]);
            String v=String.valueOf(vals[i]);
            fm=g.getFontMetrics(); g.drawString(v,px+pw-28-fm.stringWidth(v),sy+24);
        }

        int bw3=(pw-42)/3,by3=py+360,bh3=50;
        drawBtn(g,px+12,by3,bw3,bh3,"MENU",new Color(70,70,120),new Color(130,130,200));
        drawBtn(g,px+15+bw3,by3,bw3,bh3,"LANJUT",new Color(30,100,60),new Color(60,200,100));
        drawBtn(g,px+18+bw3*2,by3,bw3,bh3,"ULANG",ac,MenuScreen.lighter(ac,40));

        g.setFont(new Font("Arial",Font.PLAIN,10)); g.setColor(new Color(60,200,100,160));
        fm=g.getFontMetrics(); String hint="+5 nyawa dipulihkan";
        g.drawString(hint,px+15+bw3+(bw3-fm.stringWidth(hint))/2,by3+bh3+16);

        g.setFont(new Font("Arial",Font.PLAIN,11)); g.setColor(new Color(255,255,255,50));
        fm=g.getFontMetrics(); String esc="ESC = menu";
        g.drawString(esc,(W-fm.stringWidth(esc))/2,py+ph-8);
    }

    void drawBtn(Graphics2D g,int x,int y,int w,int h,String lbl,Color fill,Color border){
        g.setColor(MenuScreen.ca(fill,80)); g.fill(new RoundRectangle2D.Float(x,y,w,h,14,14));
        g.setColor(border); g.setStroke(new BasicStroke(1.5f)); g.draw(new RoundRectangle2D.Float(x,y,w,h,14,14));
        g.setColor(new Color(255,255,255,30)); g.fill(new RoundRectangle2D.Float(x+3,y+3,w-6,h/2-3,10,10));
        g.setFont(new Font("Arial Black",Font.BOLD,15)); FontMetrics fm=g.getFontMetrics();
        g.setColor(new Color(0,0,0,60)); g.drawString(lbl,x+(w-fm.stringWidth(lbl))/2+1,y+h/2+6);
        g.setColor(Color.WHITE); g.drawString(lbl,x+(w-fm.stringWidth(lbl))/2,y+h/2+5);
    }
}
