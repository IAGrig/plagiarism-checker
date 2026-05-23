package plagiarism.testutils

object TestFixtures:

  val vectorDimension: Int = 384

  val studentADiff: String =
    """diff --git a/solution/src/main.c b/solution/src/main.c
      |--- a/solution/src/main.c
      |+++ b/solution/src/main.c
      |@@ -1,3 +1,25 @@
      |+#include "bmp.h"
      |+#include "transform.h"
      |+#include <stdio.h>
      |+#include <string.h>
      |+
      | int main(int argc, char* argv[]) {
      |-    return 0;
      |+    if (argc != 4) {
      |+        fprintf(stderr, "Usage: %s <input> <output> <transform>\n", argv[0]);
      |+        return 1;
      |+    }
      |+    FILE* in = fopen(argv[1], "rb");
      |+    if (!in) { perror("Cannot open input"); return 2; }
      |+    struct image img;
      |+    enum read_status rs = from_bmp(in, &img);
      |+    fclose(in);
      |+    if (rs != READ_OK) { return 3; }
      |+    struct image result;
      |+    if (strcmp(argv[3], "cw90") == 0) result = rotate_cw90(img);
      |+    else if (strcmp(argv[3], "ccw90") == 0) result = rotate_ccw90(img);
      |+    else if (strcmp(argv[3], "fliph") == 0) result = flip_h(img);
      |+    else if (strcmp(argv[3], "flipv") == 0) result = flip_v(img);
      |+    else if (strcmp(argv[3], "none") == 0) result = img;
      |+    else { fprintf(stderr, "Unknown transform\n"); return 4; }
      |+    FILE* out = fopen(argv[2], "wb");
      |+    to_bmp(out, &result);
      |+    fclose(out);
      |+    return 0;
      | }""".stripMargin

  val plagiarismDIff: String =
    """diff --git a/solution/src/main.c b/solution/src/main.c
      |--- a/solution/src/main.c
      |+++ b/solution/src/main.c
      |@@ -1,3 +1,25 @@
      |+#include "bmp.h"
      |+#include "transform.h"
      |+#include <stdio.h>
      |+#include <string.h>
      |+
      | int main(int argc, char* argv[]) {
      |-    return 0;
      |+    if (argc != 4) {
      |+        fprintf(stderr, "Usage: %s <input> <output> <transform>\n", argv[0]);
      |+        return 1;
      |+    }
      |+    FILE* f_in = fopen(argv[1], "rb");
      |+    if (!f_in) { perror("Cannot open input file"); return 2; }
      |+    struct image img;
      |+    enum read_status rs = from_bmp(f_in, &img);
      |+    fclose(f_in);
      |+    if (rs != READ_OK) { return 3; }
      |+    struct image res;
      |+    if (strcmp(argv[3], "cw90") == 0) res = rotate_cw90(img);
      |+    else if (strcmp(argv[3], "ccw90") == 0) res = rotate_ccw90(img);
      |+    else if (strcmp(argv[3], "fliph") == 0) res = flip_h(img);
      |+    else if (strcmp(argv[3], "flipv") == 0) res = flip_v(img);
      |+    else if (strcmp(argv[3], "none") == 0) res = img;
      |+    else { fprintf(stderr, "Unknown transformation\n"); return 4; }
      |+    FILE* f_out = fopen(argv[2], "wb");
      |+    to_bmp(f_out, &res);
      |+    fclose(f_out);
      |+    return 0;
      | }""".stripMargin

  val studentCDiff: String =
    """diff --git a/solution/src/main.c b/solution/src/main.c
      |--- a/solution/src/main.c
      |+++ b/solution/src/main.c
      |@@ -1,3 +1,40 @@
      |+#include "image_io.h"
      |+#include "image_ops.h"
      |+#include "cli.h"
      |+#include <stdlib.h>
      |+
      | int main(int argc, char* argv[]) {
      |-    return 0;
      |+    struct cli_args args;
      |+    if (parse_args(argc, argv, &args) != CLI_OK) {
      |+        print_usage(argv[0]);
      |+        return EXIT_FAILURE;
      |+    }
      |+    struct pixel_array* pixels = NULL;
      |+    struct bmp_header header;
      |+    int status = load_bmp_file(args.input_path, &header, &pixels);
      |+    if (status != 0) {
      |+        log_error("Failed to load BMP: %s", args.input_path);
      |+        return status;
      |+    }
      |+    struct pixel_array* output = NULL;
      |+    switch (args.operation) {
      |+        case OP_ROTATE_CW:
      |+            output = apply_rotation(pixels, header.width, header.height, ROT_CW_90);
      |+            break;
      |+        case OP_ROTATE_CCW:
      |+            output = apply_rotation(pixels, header.width, header.height, ROT_CCW_90);
      |+            break;
      |+        case OP_FLIP_H:
      |+            output = apply_flip(pixels, header.width, header.height, FLIP_HORIZONTAL);
      |+            break;
      |+        case OP_FLIP_V:
      |+            output = apply_flip(pixels, header.width, header.height, FLIP_VERTICAL);
      |+            break;
      |+        case OP_NONE:
      |+            output = copy_pixels(pixels, header.width * header.height);
      |+            break;
      |+    }
      |+    save_bmp_file(args.output_path, &header, output);
      |+    free_pixels(pixels);
      |+    free_pixels(output);
      |+    return EXIT_SUCCESS;
      | }""".stripMargin

  val unrelatedDiff: String =
    """diff --git a/src/main.rs b/src/main.rs
      |--- a/src/main.rs
      |+++ b/src/main.rs
      |@@ -1,3 +1,15 @@
      |+use std::collections::HashMap;
      |+use std::io::{self, BufRead};
      |+
      | fn main() {
      |-    println!("Hello, world!");
      |+    let stdin = io::stdin();
      |+    let mut word_count: HashMap<String, usize> = HashMap::new();
      |+    for line in stdin.lock().lines() {
      |+        let line = line.expect("Failed to read line");
      |+        for word in line.split_whitespace() {
      |+            let counter = word_count.entry(word.to_lowercase()).or_insert(0);
      |+            *counter += 1;
      |+        }
      |+    }
      |+    for (word, count) in &word_count {
      |+        println!("{}: {}", word, count);
      |+    }
      | }""".stripMargin

  def fakeEmbedding(seed: Long, dim: Int = vectorDimension): Array[Float] =
    val rng = new java.util.Random(seed)
    Array.fill(dim)(rng.nextFloat() * 2 - 1)

  def cosineSimilarity(a: Array[Float], b: Array[Float]): Double =
    require(a.length == b.length, s"Vector dimensions must match: ${a.length} != ${b.length}")
    val dot   = a.zip(b).map((x, y) => x.toDouble * y.toDouble).sum
    val normA = math.sqrt(a.map(x => x.toDouble * x.toDouble).sum)
    val normB = math.sqrt(b.map(x => x.toDouble * x.toDouble).sum)
    if (normA == 0 || normB == 0) 0.0 else dot / (normA * normB)
