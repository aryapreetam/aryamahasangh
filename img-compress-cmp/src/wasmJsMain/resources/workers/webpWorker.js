// Placeholder worker; to be replaced with Squoosh WebP codec wiring.
self.onmessage = async (e) => {
    const {id, bytes, mimeType, mode, quality, targetSizeKb, maxLongEdgePx} = e.data;
    try {
        const blob = new Blob([bytes], {type: mimeType || 'image/png'});
        const bitmap = await createImageBitmap(blob);
        let w = bitmap.width, h = bitmap.height;
        if (maxLongEdgePx && Math.max(w, h) > maxLongEdgePx) {
            const scale = maxLongEdgePx / Math.max(w, h);
            w = Math.max(1, Math.round(w * scale));
            h = Math.max(1, Math.round(h * scale));
        }
        const canvas = new OffscreenCanvas(w, h);
        const ctx = canvas.getContext('2d');
        ctx.drawImage(bitmap, 0, 0, w, h);

        async function encode(q) {
            const outBlob = await canvas.convertToBlob({type: 'image/webp', quality: (q / 100)});
            const buf = await outBlob.arrayBuffer();
            return new Uint8Array(buf);
        }

        let outBytes;
        if (mode === 'quality') {
            outBytes = await encode(quality || 75);
        } else {
            const target = (targetSizeKb || 100) * 1024;
            const tolerance = Math.max(Math.floor(target * 0.05), 10 * 1024);
            let lo = 5, hi = 95, bestQ = 95, best = await encode(95);
            while (lo <= hi) {
                const mid = Math.floor((lo + hi) / 2);
                const cur = await encode(mid);
                if (cur.length <= target && cur.length <= best.length) {
                    bestQ = mid;
                    best = cur;
                }
                if (cur.length >= target - tolerance && cur.length <= target + tolerance) {
                    outBytes = cur;
                    break;
                }
                if (cur.length > target) hi = mid - 1; else lo = mid + 1;
            }
            if (!outBytes) outBytes = best;
        }

        self.postMessage({
            id,
            ok: true,
            bytes: outBytes,
            originalSize: bytes.byteLength,
            compressedSize: outBytes.length
        }, [outBytes.buffer]);
    } catch (err) {
        self.postMessage({id, ok: false, error: String(err)});
    }
};
