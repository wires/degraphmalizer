package dgm.graphs.ops;

import java.util.Random;

/**
 * Generate random strings
 */
public class RandomIdentifierGenerator
{
    final static String[] words = ("adult,aeroplane,air,aircraft,carrier,airborne,airport,album,alphabet,apple,arm," +
            "army,baby,baby,backpack,balloon,banana,bank,barbecue,bathroom,bathtub,bed,bed,bee,bible,bible,bird,bomb," +
            "book,boss,bottle,bowl,box,boy,brain,bridge,butterfly,button,cappuccino,car,car-race,carpet,carrot,cave," +
            "chair,chess board,chief,child,chisel,chocolates,church,church,circle,circus,circus,clock,clown,coffee," +
            "coffee-shop,comet,compact disc,compass,computer,crystal,cup,cycle,data base,desk,diamond,dress,drill," +
            "drink,drum,dung,ears,earth,egg,electricity,elephant,eraser,explosive,eyes,family,fan,feather,festival," +
            "film,finger,fire,floodlight,flower,foot,fork,freeway,fruit,fungus,game,garden,gas,gate,gemstone,girl," +
            "gloves,god,grapes,guitar,hammer,hat,hieroglyph,highway,horoscope,horse,hose,ice,ice-cream,insect,jet,"+
            "junk,kaleidoscope,kitchen,knife,leather,jacket,leg,library,liquid,magnet,man,map,maze,meat,meteor," +
            "microscope,milk,milkshake,mist,money,monster,mosquito,mouth,nail,navy,necklace,needle,onion," +
            "paintbrush,pants,parachute,passport,pebble,pendulum,pepper,perfume,pillow,plane,planet,pocket," +
            "post-office,potato,printer,prison,pyramid,radar,rainbow,record,restaurant,rifle,ring,robot,rock" +
            ",rocket,roof,room,rope,saddle,salt,sandpaper,sandwich,satellite,school,sex,ship,shoes,shop,shower" +
            ",signature,skeleton,slave,snail,software,solid,space shuttle,spectrum,sphere,spice,spiral,spoon," +
            "sports-car,spot light,square,staircase,star,stomach,sun,sunglasses,surveyor,swimming pool,sword," +
            "table,tapestry,teeth,telescope,television,tennis,racket,thermometer,tiger,toilet,tongue,torch," +
            "torpedo,train,treadmill,triangle,tunnel,typewriter,umbrella,vacuum,vampire,videotape,vulture,water," +
            "weapon,web,wheelchair,window,woman,worm,x-ray,hullabaloo,sponge,idiopathic,bobbin,bamboo,poppycock," +
            "persnickety,irked,queer,flabbergasted,frippery,befuddlement,haberdashery,diphthong,britches,scrumptious," +
            "sassafras,gadabouts,bazooka,cockamamie,egad,frumpy,claptrap,pooch,sack,sag,baffled,bubbles,noodles," +
            "flagellum,blimp,napkin,jiggle,discombobulate,fallopian,pants,follicle,box,bladder,spoon,centipede," +
            "indubitably,banana,igloo,waddle,wobble,sludge,briefs,trump,gristle,sprout,turnip,gash,sandals,crunch," +
            "turd,gauze,goon,manhole,cockamamie,noddle,pudding,strudel,rubbish,duty,guava,smashing,hunky,inevitable," +
            "inedible,goon,doughnut,chicken,pickle,bubbles,blubber,sickle,miscellaneous,flagella,cilia,tweezers," +
            "jiggle,pregnant,hippo,blubber,fig,floppy,peduncle,fat,bum,perpendicular,ninja,flannel,graze,gullet," +
            "lozenge,topple,scribble,magma,bulbous,spatula,machete,cougar,rice,cheese,fillet,bacon,truffles,scruffy," +
            "sausage,bowl,flabbergasted,haberdashery,shenanigans,pop,termites,ding,feline,canine,rustic,crook," +
            "reservoir,face,booty,pony,snap,rear,moose,cashew,rummage").replace(" ",",").split(",");

    public final static String randomString()
    {
        return randomString(new Random(System.currentTimeMillis()).nextInt(Integer.MAX_VALUE));
    }

    // no guarantee about randomness
    public final static String randomWord(int index)
    {
        return words[index % words.length];
    }

    // more likely random
    public final static String randomString(int index)
    {
        // use radix 36 to generate random string, append it
        final String rndStr = Integer.toString(index, 36);

        return randomWord(index) + capitalize(rndStr);
    }

    public final static String capitalize(String s)
    {
        return s.substring(0, 1).toUpperCase() + s.substring(1, s.length());
    }
}