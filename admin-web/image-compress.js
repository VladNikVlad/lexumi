/** Client-side mirror of util/ImageCompressor.kt: shrinks a picked image file down to at most
 * [maxBytes] raw bytes (before base64) — first by lowering JPEG quality, then by shrinking
 * dimensions too if quality alone isn't enough — and returns it as a base64 string ready for a
 * `image_data` column. Resolves to null if the file can't be decoded as an image. */
export function compressImageToBase64(file, maxBytes = 100 * 1024) {
  return new Promise((resolve) => {
    const img = new Image();
    const objectUrl = URL.createObjectURL(file);
    img.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      resolve(null);
    };
    img.onload = () => {
      URL.revokeObjectURL(objectUrl);
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');
      let width = img.naturalWidth;
      let height = img.naturalHeight;

      const render = (w, h) => {
        canvas.width = w;
        canvas.height = h;
        ctx.clearRect(0, 0, w, h);
        ctx.drawImage(img, 0, 0, w, h);
      };
      const toDataUrlBytes = (quality) => {
        const dataUrl = canvas.toDataURL('image/jpeg', quality / 100);
        const base64 = dataUrl.slice(dataUrl.indexOf(',') + 1);
        // base64 is ~4/3 the size of the raw bytes it encodes — decode the length back out.
        const rawBytes = Math.floor((base64.length * 3) / 4);
        return { base64, rawBytes };
      };

      render(width, height);
      let quality = 90;
      let result = toDataUrlBytes(quality);

      while (result.rawBytes > maxBytes && quality > 35) {
        quality -= 10;
        result = toDataUrlBytes(quality);
      }

      while (result.rawBytes > maxBytes && (width > 120 || height > 120)) {
        width = Math.max(120, Math.floor(width * 0.8));
        height = Math.max(120, Math.floor(height * 0.8));
        render(width, height);
        quality = 80;
        result = toDataUrlBytes(quality);
        while (result.rawBytes > maxBytes && quality > 35) {
          quality -= 10;
          result = toDataUrlBytes(quality);
        }
      }

      resolve(result.base64);
    };
    img.src = objectUrl;
  });
}
