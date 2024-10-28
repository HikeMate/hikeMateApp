package ch.hikemate.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.hikemate.app.ui.theme.primaryColor

enum class ButtonType(
    val backgroundColor: Color,
    val textColor: Color,
    val border: BorderStroke
) {
    PRIMARY(primaryColor, Color.White, BorderStroke(0.dp, Color.Transparent)),
    SECONDARY(Color.White, Color.Black, BorderStroke(1.dp, Color.Black)),
}

@Composable
fun BigButton(buttonType: ButtonType,
    label: String
              , onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            contentColor = buttonType.textColor,
            containerColor = buttonType.backgroundColor
        ),
        shape = RoundedCornerShape(20),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
        .border(
            buttonType.border,
            shape = RoundedCornerShape(20)
        )
    ) {
        Text(text = label, style = TextStyle(
            color = buttonType.textColor,
            fontSize = 20.sp,
            fontWeight = FontWeight(600)
        ))
    }
}