package cz.humblej.squares.network;

import cz.humblej.squares.model.PlayerProfile;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NetworkProtocolTest {
    @Test
    public void valuesAndProfilesRoundTripThroughWireFormat() {
        String message = "Příliš žluťoučký kůň";
        assertEquals(message, NetworkProtocol.decodeValue(NetworkProtocol.encodeValue(message)));

        PlayerProfile profile = PlayerProfile.create("Síťový hráč");
        PlayerProfile decoded = NetworkProtocol.decodeProfile(NetworkProtocol.encodeProfile(profile));
        assertEquals(profile.id(), decoded.id());
        assertEquals(profile.displayName(), decoded.displayName());
        assertEquals(profile.createdAt().toEpochMilli(), decoded.createdAt().toEpochMilli());
    }

    @Test
    public void invalidBase64DecodesToEmptyValueLikeOriginalProtocol() {
        assertEquals("", NetworkProtocol.decodeValue("%%%"));
    }
}
