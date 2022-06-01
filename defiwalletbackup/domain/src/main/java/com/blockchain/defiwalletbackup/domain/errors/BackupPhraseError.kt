package com.blockchain.defiwalletbackup.domain.errors

sealed interface BackupPhraseError{
    object NoMnemonicFound: BackupPhraseError
    object BackupConfirmationError: BackupPhraseError
}
