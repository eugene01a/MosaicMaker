import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Reads the EXIF Orientation tag (TIFF tag 0x0112, in IFD0) from a JPEG's APP1 segment
 * and rotates the image to match.
 */
final class ExifOrientation {

    private ExifOrientation() {
    }

    static int readOrientation(File file) {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            if (in.readUnsignedShort() != 0xFFD8) {
                return 1;
            }
            while (true) {
                int marker = in.readUnsignedShort();
                if ((marker & 0xFF00) != 0xFF00 || marker == 0xFFDA) {
                    break; // not a marker, or start of scan: no more metadata segments
                }
                int length = in.readUnsignedShort();
                byte[] data = new byte[length - 2];
                in.readFully(data);
                if (marker == 0xFFE1) {
                    Integer orientation = parseExifOrientation(data);
                    if (orientation != null) {
                        return orientation;
                    }
                }
            }
        } catch (IOException ex) {
            // Not a readable JPEG, or truncated - fall through to default orientation.
        }
        return 1;
    }

    private static Integer parseExifOrientation(byte[] data) {
        if (data.length < 10 || data[0] != 'E' || data[1] != 'x' || data[2] != 'i' || data[3] != 'f') {
            return null;
        }
        int tiffStart = 6; // skip "Exif\0\0"
        // TIFF declares its own byte order (big/little endian)
        boolean bigEndian = data[tiffStart] == 'M';
        int ifdStart = tiffStart + readInt32(data, tiffStart + 4, bigEndian);
        if (ifdStart < 0 || ifdStart + 2 > data.length) {
            return null;
        }
        int numEntries = readInt16(data, ifdStart, bigEndian);
        for (int i = 0; i < numEntries; i++) {
            int entryOffset = ifdStart + 2 + i * 12;
            if (entryOffset + 12 > data.length) {
                break;
            }
            if (readInt16(data, entryOffset, bigEndian) == 0x0112) {
                return readInt16(data, entryOffset + 8, bigEndian);
            }
        }
        return null;
    }

    private static int readInt16(byte[] data, int offset, boolean bigEndian) {
        int b0 = data[offset] & 0xFF;
        int b1 = data[offset + 1] & 0xFF;
        return bigEndian ? (b0 << 8) | b1 : (b1 << 8) | b0;
    }

    private static int readInt32(byte[] data, int offset, boolean bigEndian) {
        return bigEndian
                ? (readInt16(data, offset, true) << 16) | readInt16(data, offset + 2, true)
                : (readInt16(data, offset + 2, false) << 16) | readInt16(data, offset, false);
    }

    /** Handles the rotations that occur in practice (1/3/6/8); mirrored orientations are left as-is. */
    static BufferedImage apply(BufferedImage image, int orientation) {
        int degrees = switch (orientation) {
            case 3 -> 180;
            case 6 -> 90;
            case 8 -> 270;
            default -> 0;
        };
        if (degrees == 0) {
            return image;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        boolean swapDimensions = degrees != 180;
        int newWidth = swapDimensions ? height : width;
        int newHeight = swapDimensions ? width : height;

        BufferedImage result = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = result.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.translate(newWidth / 2.0, newHeight / 2.0);
        g2d.rotate(Math.toRadians(degrees));
        g2d.translate(-width / 2.0, -height / 2.0);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return result;
    }
}
