
require_relative 'RSDL.rb'
include SDL2


class Card
    @@WIDTH=97
    @@HEIGHT=129
    @@texture=nil
    @@cardback=nil
    @@FACE_DOWN=0
    @@FACE_UP=1
    @@TURNING_UP=2
    @@TURNING_DOWN=3
    @@FLYING_AWAY=4
    @@DIAMONDS=0
    @@HEARTS=1
    @@SPADES=2
    @@CLUBS=3
    @@RED=0x0000ff
    @@GREEN=0x00ff00
    @@BLUE=0xff0000
    @@ALPHA=0xff000000

    def self.WIDTH
        @@WIDTH
    end
    
    def self.HEIGHT
        @@HEIGHT
    end
    
    def rank
        @rank
    end
    def suit
        @suit
    end
    
    def initialize(rank,suit)
        @rank=rank
        @suit=suit
        @x=0
        @y=0
        @state=@@FACE_DOWN
        @progress=0
        
        if @@texture == nil
            @@texture = SDL2.SDL_LoadBMP("allcards.bmp")
            pixf = SDL2::SDL_PixelFormat.malloc()
            pixf.format = SDL_PIXELFORMAT_ABGR8888
            pixf.palette = nil
            pixf.BitsPerPixel = 32
            pixf.BytesPerPixel = 4
            pixf.Rmask = @@RED
            pixf.Gmask = @@GREEN
            pixf.Bmask = @@BLUE
            pixf.Amask= @@ALPHA
            @@texture = SDL2.SDL_ConvertSurface(@@texture,pixf.to_ptr(),0)
            SDL2.SDL_SetColorKey(@@texture,1,0x010101)
            
            @@cardback = SDL2.SDL_CreateRGBSurface(0,@@WIDTH,@@HEIGHT,
                32, @@RED,@@GREEN,@@BLUE,@@ALPHA)
            r = SDL2::SDL_Rect.malloc()
            r.x=4*@@WIDTH
            r.y=4*@@HEIGHT
            r.w=@@WIDTH
            r.h=@@HEIGHT
            SDL2.SDL_BlitSurface(@@texture,r.to_ptr,@@cardback,nil)
        end

        @spr = SDL2.SDL_CreateRGBSurface(0,@@WIDTH, @@HEIGHT, 32,
                @@RED,@@GREEN,@@BLUE,@@ALPHA)
        r = SDL2::SDL_Rect.malloc()
        if rank == 14
            r.x = 0
        else
            r.x = (rank-1)*@@WIDTH
        end
        r.y = suit*@@HEIGHT
        r.w = @@WIDTH
        r.h = @@HEIGHT
        SDL2.SDL_BlitSurface( @@texture, r.to_ptr, @spr, nil )
    end
    
        
    def clone()
        c=Card.new(@rank,@suit)
        c.setPosition(@x,@y)
        c.setState(@state)
        c.setProgress(@progress)
        return c
    end

    def setState(state)
        @state=state
    end
    
    def setProgress(p)
        @progress=p
    end
    
    def setPosition(x,y)
        @x=x
        @y=y
    end
    
    def update()
        if @state == @@TURNING_DOWN or @state == @@TURNING_UP
            @progress+=10
            if( @progress >= 100 )
                @progress = 0
                if( @state == @@TURNING_UP )
                    @state = @@FACE_UP
                else
                    @state = @@FACE_DOWN
                end
            end
        elsif( @state == @@FLYING_AWAY )
            @progress += 10
            if( @progress >= 100 )
                @progress = 100
            end
        end
    end
    
    def isFaceUp()
        return (@state == @@FACE_UP or @state == @@TURNING_UP)
    end
    
    def containsPoint(x,y)
        if( @state == @@FLYING_AWAY )
            return false
        end
        if( x >= @x and y >= @y and x <= @x + @@WIDTH and y <= @y + @@HEIGHT )
            return true
        else
            return false
        end
    end
    
    def flip()
        if( @state == @@FACE_DOWN )
            @state = @@TURNING_UP
            @progress = 0
        elsif( @state == @@FACE_UP )
            @state = @@TURNING_DOWN
            @progress = 0
        elsif( @state == @@TURNING_UP )
            @state = @@TURNING_DOWN
            @progress = 100-@progress
        elsif( @state == @@TURNING_DOWN )
            @state = @@TURNING_UP
            @progress = 100-@progress
        end
    end
    
    def flyAway()
        @state=@@FLYING_AWAY
        @progress = 0
    end
    
    def draw(win)
        
        sx=1.0
        alpha=1.0
        x=@x
        y=@y
        
        if( @state == @@FLYING_AWAY )
            s = @spr
            alpha = (1.0-@progress/100.0)
        else
            if( @state == @@FACE_DOWN or (@state == @@TURNING_UP and @progress < 50 ) or (@state == @@TURNING_DOWN and @progress > 50)  )
                s = @@cardback
            else
                s = @spr
            end

            if( @progress < 50 )
                sx = (50-@progress)/50.0
            else
                sx = (@progress-50)/50.0;
            end
        end
        
        if alpha == 0.0
            return
        end
            
        SDL2.SDL_SetSurfaceAlphaMod(s, (alpha*255))
        
        r = SDL2::SDL_Rect.malloc()
        r.x = x
        r.y = y
        r.w = (sx * @@WIDTH )
        r.h = @@HEIGHT
        if sx != 1.0
            r.x += (@@WIDTH/2 - sx*@@WIDTH/2)
            SDL2.SDL_BlitScaled( s, nil, win, r.to_ptr() )
        else
            SDL2.SDL_BlitSurface(s,nil,win,r.to_ptr)
        end
    end
end
        
