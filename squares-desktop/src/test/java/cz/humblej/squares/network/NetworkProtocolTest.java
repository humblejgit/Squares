package cz.humblej.squares.network;

import cz.humblej.squares.model.PlayerProfile;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NetworkProtocolTest {
    @Test
    public void valuesAndProfilesRoundTripThroughWireFormat() {
        String message = "PĹ™Ă­liĹˇ ĹľluĹĄouÄŤkĂ˝ kĹŻĹ";
        assertEquals(message, NetworkProtocol.decodeValue(NetworkProtocol.encodeValue(message)));

        PlayerProfile profile = PlayerProfile.create("SĂ­ĹĄovĂ˝ hrĂˇÄŤ");
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
