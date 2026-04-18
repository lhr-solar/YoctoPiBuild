# Slim ffmpeg down to only what photon-dashboard needs: H.264 software decode
# and YUV->RGBA scaling. Cuts libavcodec from ~40MB to ~6MB on aarch64.
#
# We strip everything via --disable-everything, then re-enable just:
#   - h264 decoder (parser + demux pulled implicitly)
#   - swscale (used for YUV420P -> RGBA conversion)
#   - shared libs (we link libavcodec/libavutil/libswscale at runtime)
# The ffmpeg/ffplay/ffprobe CLIs and libavformat/libavfilter are dropped.

EXTRA_OECONF:append = " \
    --disable-everything \
    --disable-doc \
    --disable-htmlpages \
    --disable-manpages \
    --disable-podpages \
    --disable-txtpages \
    --disable-programs \
    --disable-avdevice \
    --disable-avformat \
    --disable-avfilter \
    --disable-postproc \
    --disable-network \
    --disable-static \
    --enable-shared \
    --enable-swscale \
    --enable-decoder=h264 \
    --enable-parser=h264 \
"

# libavformat/avfilter are gone, so don't ship their packages either.
PACKAGES:remove = "libavformat libavfilter libpostproc libavdevice libavresample"
