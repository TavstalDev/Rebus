package com.github.TavstalDev.rebus.config.models;

import com.github.TavstalDev.rebus.config.RebusConfig;
import lombok.Getter;

@Getter
public class NpcConfig {
    private final String name;
    private final String skin;
    private final String signature;

    public NpcConfig(RebusConfig parent) {
        name = parent.resolveGet("npc.name", "&f_Rébusz_");
        skin = parent.resolveGet("npc.skin", "ewogICJ0aW1lc3RhbXAiIDogMTc0MDU4MzQxMzMyMiwKICAicHJvZmlsZUlkIiA6ICJiZDNhNWRmY2ZkZjg0NDczOTViZDJiZmUwNGY0YzAzMiIsCiAgInByb2ZpbGVOYW1lIiA6ICJwcmVja3Jhc25vIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2Y4MzkzMGY3MWJmMjVkZDEzYjM2NGZmN2UwZTg3ZTg4Yjg3NTZjYmJiZjgyMjQxMGY0NzA0NWVjZjEyNzYzOTkiCiAgICB9CiAgfQp9");
        signature = parent.resolveGet("npc.signature", "s0iYOKakbeeLI3bIxokioac+R4yNiyYYkaubIfe5pBXJgUbFpxbVV40M/G7huGPMwbi6gHI6PgD4Q8n4BiGqWYcxwQ227n0MEnvQIerJgvB1gm/49BrgQ565ldixYWNQwHh0jkxErAVRqyd7Kb5vGup1Ba4QB7E2c4+kQnyDyBDHZ8RKc11EWIevdB5NZcrGLUON34mVy9D7wZihyUOOAvcAC8KrGX6wmvizZqcNtRTcymmNF1t7Zl+rxQxsBou3qMSze3RmDuhSKKy7oIjYc3pbag2uHAdhjWqEMI3qUkN2wADzr11vNYhqhs+uOKegHJMuWkMeeThEje20iMdpnz7Ut9VOZe+imPhHOX07Yi5mQblptBtYJkfhIOeRoh65Zv9w1aspks/KkSiRud0mpdr7ky8TmUWTy+YZgernKIWwuYk5YoxIXMcojmXsMUzWDV719ctbqncxfiaa3fvF2/IoI3AUn6w1Fc04sbcu54XiNOTR2WrdS8accwyX9zEzSqdvYKFl3bPshH6qKtf57d7V0VsGIwGfUU5wtfWjH6G1ylZFmi8ji1o1O/X5LV1R1bw1WsuAyN5GhyXWq8A+PhUEsG+/lH+qJ4WDvFSqmb/f7fEMAZG2kUW/ye823RSjchqXMy78i5pneS98DBGoO5MjG2T1yo9Rp6NWsVHHO0o=");
    }
}
