/**
 * GameEngine.java
 * @author Greg (Prime) Galligan
 */
package gp.galligan.game;

import gp.galligan.game.gfx.Colors;
import gp.galligan.game.gfx.Screen;
import gp.galligan.game.gfx.SpriteSheet;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import javax.swing.JFrame;

@SuppressWarnings("serial")
public class GameEngine extends Canvas implements Runnable {

	private static final String     TITLE       = "Olimu Engine Alpha";
	private static final int        WIDTH       = 850;
	private static final int        HEIGHT      = WIDTH * 3/4;
	private static final Dimension  DIMENSION   = new Dimension(WIDTH, HEIGHT);
	private static final int        UPDATE_RATE = 30;
	private static final int        RENDER_RATE = 60;
	
	private JFrame _frame;
	private Thread _thread;
	private int    _tps        = 0;
	private int    _fps        = 0;
	private int    _totalTicks = 0;
	
	private BufferedImage _image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
	private int[] pixels = ((DataBufferInt)_image.getRaster().getDataBuffer()).getData();
	private int[] colors = new int[6*6*6];
	
	private Screen screen;
	private InputHandler inputHandler;
	private boolean _running   = false;
	
	/**
	 * Singleton instantiation of GameEngine
	 */
	private static final GameEngine _engine = new GameEngine();

	/**
	 * Private constructor for GameEngine
	 */
	private GameEngine() {
		setMinimumSize(DIMENSION);
		setMaximumSize(DIMENSION);
		setPreferredSize(DIMENSION);
		
		_frame = new JFrame(TITLE);
		_frame.setLayout(new BorderLayout());
		_frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		_frame.add(this, BorderLayout.CENTER);
		_frame.pack();
		
		_frame.setLocationRelativeTo(null);
		_frame.setResizable(false);
		_frame.setVisible(true);
	}

	/**
	 * Initialize resources
	 */
	public void init() {
		int index = 0;
		for(int r=0; r<6; r++) {
			for(int g=0; g<6; g++) {
				for(int b=0; b<6; b++) {
					int rr = (r * 255/5);
					int gg = (g * 255/5);
					int bb = (b * 255/5);
					
					colors[index++] = rr << 16 | gg << 8 | bb;
				}
			}
		}
		
		screen = new Screen(WIDTH, HEIGHT, new SpriteSheet("/spritesheet.png"));
		inputHandler = new InputHandler(_engine);
	}

	/**
	 * Main running loop of the engine
	 */
	@Override
	public void run() {
		double lastUpdateTime = System.nanoTime();
		double lastRenderTime = lastUpdateTime;

		final int    ns = 1000000000;
		final double nsPerUpdate = (double)(ns)/UPDATE_RATE;
		final double nsPerRender = (double)(ns)/RENDER_RATE;
		final int    maxUpdatePerRender = 5;
		
		int lastSecond  = (int)(lastUpdateTime/ns);
		int tickCount   = 0;
		int renderCount = 0;
		
		init();
		System.out.println("Game Engine started!");
		while(_running) {
			long currentTime = System.nanoTime();
			int tps = 0;
			
			while((currentTime-lastUpdateTime)>nsPerUpdate && tps<maxUpdatePerRender) {
				update(tickCount);
				++_totalTicks;
				++tickCount;
				++tps;
				
				lastUpdateTime += nsPerUpdate;
			}
			
			if((currentTime-lastUpdateTime)>nsPerUpdate) {
				lastUpdateTime = currentTime-nsPerUpdate;
			}
			
			float interp = Math.min(1.0F, (float)((currentTime-lastUpdateTime)/nsPerUpdate));
			render(interp);
			++renderCount;
			lastRenderTime = currentTime;
			
			int currentSecond = (int)(lastUpdateTime/ns);
			if(currentSecond>lastSecond) {
				_tps = tickCount;
				_fps = renderCount;
				tickCount   = 0;
				renderCount = 0;
				lastSecond  = currentSecond;
				System.out.println("TPS: " + _tps + "    FPS: " + _fps);
			}
			
			while((currentTime-lastRenderTime)<nsPerRender && (currentTime-lastUpdateTime)<nsPerUpdate) {
				Thread.yield();
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				currentTime = System.nanoTime();
			}
			
		}

	}
	
	/**
	 * Starts the dedicated engine thread
	 */
	public synchronized void start() {
		_running = true;
		_thread  = new Thread(this, TITLE + "_main");
		_thread.start();
	}
	
	/**
	 * Stops the dedicated engine thread
	 */
	public synchronized void stop() {
		_running = false;
		if(_thread!=null)
			try {
				_thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

	/**
	 * Called once per frame, checks for input and updates the screen
	 */
	public void update(int tickCount) {
		if(inputHandler.keyUp.isPressed()) { screen.yOffset--; }
		if(inputHandler.keyDown.isPressed()) { screen.yOffset++; }
		if(inputHandler.keyLeft.isPressed()) { screen.xOffset--; }
		if(inputHandler.keyRight.isPressed()) { screen.xOffset++; }
	}

	/**
	 * Renders screen imagery
	 */
	public void render(float interp) {
		BufferStrategy bs = getBufferStrategy();
		if(bs==null) {
			createBufferStrategy(3);
			return;
		}
		
		for(int y=0; y<32; y++) {
			for(int x=0; x<32; x++) {
				screen.render(x<<3, y<<3, 0, Colors.getColor(555, 500, 050, 005));
			}
		}
		
		for(int y=0; y<screen.height; y++) {
			for(int x=0; x<screen.width; x++) {
				int colorCode = screen.pixels[x+y*screen.width];
				if(colorCode < 255) { pixels[x+y * WIDTH] = colors[colorCode]; }
			}
		}
			
		Graphics g = bs.getDrawGraphics();
		g.drawRect(0, 0, getWidth(), getHeight());
		g.drawImage(_image, 0, 0, getWidth(), getHeight(), null);
		
		g.dispose();
		bs.show();
	}

	/**
	 * Checks for input with every call to update
	 */
	/*public void checkInput() {
		if(_input.isKeyDown(KeyEvent.VK_RIGHT)) {
			_gs.getPlayer().moveX(1);
		}

		if(_input.isKeyDown(KeyEvent.VK_LEFT)) {
			_gs.getPlayer().moveX(-1);
		}

		if(_input.isKeyDown(KeyEvent.VK_UP)) {
			_gs.getPlayer().moveY(-1);
		}

		if(_input.isKeyDown(KeyEvent.VK_DOWN)) {
			_gs.getPlayer().moveY(1);
		}
	}*/

	/**
	 * Starts the game engine and loads to the menu
	 * @param String[] args, unused.
	 */
	public static void main(String[] args) {
		System.out.println("Starting Game Engine...");
		_engine.start();
	}

}
