package com.y271727uy.FRMC.capability.blockchecking.model;

/** Broad server work categories that future instrumentation may identify. */
public enum WorkKind {
    SERVER_TICK,
    CHUNK_TICK,
    ENTITY_TICK,
    BLOCK_ENTITY_TICK,
    GAME_LOGIC,
    ENTITY_AI,
    UNKNOWN
}
