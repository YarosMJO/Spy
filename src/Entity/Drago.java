package Entity;

import Audio.AudioPlayer;
import TileMap.TileMap;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class Drago extends MapObject implements Hero {

    // animation actions
    private static final int IDLE = 0;
    private static final int WALKING = 1;
    private static final int JUMPING = 2;
    private static final int FALLING = 3;
    private static final int GLIDING = 4;
    private static final int FIREBALL = 5;
    private static final int SCRATCHING = 6;
    private final int maxHealth;
    private final int maxFire;
    private final int fireCost;
    private final int fireBallDamage;
    private final int[] numFrames = {
            2, 8, 1, 2, 4, 2, 5
    };
    // player stuff
    private int health;
    private int fire;
    private boolean dead;
    private boolean flinching;
    private long flinchTimer;
    // fireball
    private boolean firing;
    private ArrayList<FireBall> fireBalls;
    // scratch
    private boolean scratching;
    private int scratchDamage;
    private int scratchRange;
    // gliding
    private boolean gliding;
    // animations
    private ArrayList<BufferedImage[]> sprites;
    private HashMap<String, AudioPlayer> sfx;

    public Drago(TileMap tm) {

        super(tm);

        width = 30;
        height = 30;
        cwidth = 20;
        cheight = 20;

        moveSpeed = 0.3;
        maxSpeed = 1.6;
        stopSpeed = 0.4;
        fallSpeed = 0.15;
        maxFallSpeed = 4.0;
        jumpStart = -4.8;
        stopJumpSpeed = 0.3;

        facingRight = true;

        health = maxHealth = 5;
        fire = maxFire = 2500;

        fireCost = 200;
        fireBallDamage = 5;
        fireBalls = new ArrayList<>();

        scratchDamage = 8;
        scratchRange = 40;

        // load sprites
        try {

            BufferedImage spritesheet = ImageIO.read(
                    getClass().getResourceAsStream(
                            "/Sprites/Player/playersprites.gif"
                    )
            );

            sprites = new ArrayList<>();
            for (int i = 0; i < 7; i++) {

                BufferedImage[] bi
                        = new BufferedImage[numFrames[i]];

                for (int j = 0; j < numFrames[i]; j++) {

                    if (i != SCRATCHING) {
                        bi[j] = spritesheet.getSubimage(
                                j * width,
                                i * height,
                                width,
                                height
                        );
                    } else {

                        bi[j] = spritesheet.getSubimage(
                                j * width * 2,
                                i * height,
                                width * 2,
                                height
                        );
                    }

                }

                sprites.add(bi);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        animation = new Animation();
        currentAction = IDLE;
        animation.setFrames(sprites.get(IDLE));
        animation.setDelay(400);
        sfx = new HashMap<>();

        sfx.put("jump", new AudioPlayer("/SFX/jump.wav"));
        sfx.put("scratch", new AudioPlayer("/SFX/scratch.wav"));
    }

    @Override
    public int getHealth() {
        return health;
    }

    @Override
    public void setHealth(int health) {
        this.health = health;
    }

    @Override
    public int getMaxHealth() {
        return maxHealth;
    }

    @Override
    public int getFire() {
        return fire;
    }

    @Override
    public int getMaxFire() {
        return maxFire;
    }

    public boolean isDead() {
        return dead;
    }

    @Override
    public void setDead(boolean dead) {
        this.dead = dead;
    }

    @Override
    public void setFiring() {
        firing = true;
    }

    @Override
    public void setScratching() {
        scratching = true;
    }

    @Override
    public void setGliding(boolean b) {
        gliding = b;
    }

    @Override
    public void checkAttack(ArrayList<Enemy> enemies) {

        // loop through enemies
        for (Enemy e : enemies) {

            // scratch attack
            if (scratching) {

                if (facingRight) {
                    if (e.getx() > x
                            && e.getx() < x + scratchRange
                            && e.gety() > y - height / 2
                            && e.gety() < y + height / 2) {
                        e.hit(scratchDamage);
                    }
                } else {
                    if (e.getx() < x
                            && e.getx() > x - scratchRange
                            && e.gety() > y - height / 2
                            && e.gety() < y + height / 2) {
                        e.hit(scratchDamage);
                    }
                }
            }

            // fireballs
            for (FireBall fireBall : fireBalls) {
                if (fireBall.intersects(e)) {
                    e.hit(fireBallDamage);
                    fireBall.setHit();
                    break;
                }
            }

            // check enemy collision
            if (intersects(e)) {
                hit(e.getDamage());
            }

        }

    }

    @Override
    public void hit(int damage) {
        if (flinching) {
            return;
        }
        stop();
        health -= damage;
        if (health < 0) {
            health = 0;
        }
        if (health == 0) {
            dead = true;
        }
        flinching = true;
        flinchTimer = System.nanoTime();

    }

    @Override
    public void getNextPosition() {

        // movement
        if (left) {
            dx -= moveSpeed;
            if (dx < -maxSpeed) {
                dx = -maxSpeed;
            }
        } else if (right) {
            dx += moveSpeed;
            if (dx > maxSpeed) {
                dx = maxSpeed;
            }
        } else {
            if (dx > 0) {
                dx -= stopSpeed;
                if (dx < 0) {
                    dx = 0;
                }
            } else if (dx < 0) {
                dx += stopSpeed;
                if (dx > 0) {
                    dx = 0;
                }
            }
        }

        // cannot move while attacking, except in air
        if ((currentAction == SCRATCHING || currentAction == FIREBALL)
                && !(jumping || falling)) {
            dx = 0;
        }

        // jumping
        if (jumping && !falling) {
            sfx.get("jump").play();
            dy = jumpStart;
            falling = true;
        }

        // falling
        if (falling) {

            if (dy > 0 && gliding) {
                dy += fallSpeed * 0.1;
            } else {
                dy += fallSpeed;
            }

            if (dy > 0) {
                jumping = false;
            }
            if (dy < 0 && !jumping) {
                dy += stopJumpSpeed;
            }

            if (dy > maxFallSpeed) {
                dy = maxFallSpeed;
            }

        }

    }

    @Override
    public void update() {
        // update position
        getNextPosition();
        checkTileMapCollision();
        setPosition(xtemp, ytemp);

        // check attack has stopped
        if (currentAction == SCRATCHING) {
            if (animation.hasPlayedOnce()) {
                scratching = false;
            }
        }
        if (currentAction == FIREBALL) {
            if (animation.hasPlayedOnce()) {
                firing = false;
            }
        }

        // fireball attack
        fire += 1;
        if (fire > maxFire) {
            fire = maxFire;
        }
        if (firing && currentAction != FIREBALL) {
            if (fire > fireCost) {
                fire -= fireCost;
                FireBall fb = new FireBall(tileMap, facingRight);
                fb.setPosition(x, y);
                fireBalls.add(fb);
            }
        }

        // update fireballs
        for (int i = 0; i < fireBalls.size(); i++) {
            fireBalls.get(i).update();
            if (fireBalls.get(i).shouldRemove()) {
                fireBalls.remove(i);
                i--;
            }
        }

        // check done flinching
        if (flinching) {
            long elapsed
                    = (System.nanoTime() - flinchTimer) / 1000000;
            if (elapsed > 1000) {
                flinching = false;
            }
        }

        // set animation
        if (scratching) {
            if (currentAction != SCRATCHING) {
                sfx.get("scratch").play();
                currentAction = SCRATCHING;
                animation.setFrames(sprites.get(SCRATCHING));
                animation.setDelay(50);
                width = 60;
            }
        } else if (firing) {
            if (currentAction != FIREBALL) {
                currentAction = FIREBALL;
                animation.setFrames(sprites.get(FIREBALL));
                animation.setDelay(100);
                width = 30;
            }
        } else if (dy > 0) {
            if (gliding) {
                if (currentAction != GLIDING) {
                    currentAction = GLIDING;
                    animation.setFrames(sprites.get(GLIDING));
                    animation.setDelay(100);
                    width = 30;
                }
            } else if (currentAction != FALLING) {
                currentAction = FALLING;
                animation.setFrames(sprites.get(FALLING));
                animation.setDelay(100);
                width = 30;
            }
        } else if (dy < 0) {
            if (currentAction != JUMPING) {
                currentAction = JUMPING;
                animation.setFrames(sprites.get(JUMPING));
                animation.setDelay(-1);
                width = 30;
            }
        } else if (left || right) {
            if (currentAction != WALKING) {
                currentAction = WALKING;
                animation.setFrames(sprites.get(WALKING));
                animation.setDelay(40);
                width = 30;
            }
        } else {
            if (currentAction != IDLE) {
                currentAction = IDLE;
                animation.setFrames(sprites.get(IDLE));
                animation.setDelay(400);
                width = 30;
            }
        }

        animation.update();

        // set direction
        if (currentAction != SCRATCHING && currentAction != FIREBALL) {
            if (right) {
                facingRight = true;
            }
            if (left) {
                facingRight = false;
            }
        }
    }

    @Override
    public void draw(Graphics2D g) {

        setMapPosition();

        // draw fireballs
        for (FireBall fireBall : fireBalls) {
            fireBall.draw(g);
        }

        // draw player
        if (flinching) {
            long elapsed = (System.nanoTime() - flinchTimer) / 1000000;
            if (elapsed / 100 % 2 == 0) {
                return;
            }
        }
        super.draw(g);
    }

    @Override
    public void reset() {
        health = maxHealth;
        facingRight = true;
        currentAction = IDLE;
        stop();
    }

    @Override
    public void stop() {

        left = right = up = down = flinching = jumping = scratching = false;

    }

}
